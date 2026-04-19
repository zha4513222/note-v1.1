package com.example.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.note.dto.NoteCreateDTO;
import com.example.note.dto.NoteVO;
import com.example.note.dto.KeywordVO;
import com.example.note.entity.*;
import com.example.note.repository.CategoryMapper;
import com.example.note.repository.KeywordMapper;
import com.example.note.repository.NoteImageMapper;
import com.example.note.repository.NoteMapper;
import com.example.note.repository.NoteTagMapper;
import com.example.note.repository.TagMapper;
import com.example.note.repository.UserTagHistoryMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final TagMapper tagMapper;
    private final KeywordMapper keywordMapper;
    private final NoteTagMapper noteTagMapper;
    private final NoteImageMapper noteImageMapper;
    private final UserTagHistoryMapper userTagHistoryMapper;
    private final NLPService nlpService;
    private final TagService tagService;
    private final CategoryMapper categoryMapper;

    public NoteServiceImpl(NoteMapper noteMapper, TagMapper tagMapper,
                          KeywordMapper keywordMapper, NoteTagMapper noteTagMapper,
                          NoteImageMapper noteImageMapper,
                          UserTagHistoryMapper userTagHistoryMapper,
                          NLPService nlpService, TagService tagService,
                          CategoryMapper categoryMapper) {
        this.noteMapper = noteMapper;
        this.tagMapper = tagMapper;
        this.keywordMapper = keywordMapper;
        this.noteTagMapper = noteTagMapper;
        this.noteImageMapper = noteImageMapper;
        this.userTagHistoryMapper = userTagHistoryMapper;
        this.nlpService = nlpService;
        this.tagService = tagService;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public NoteVO createNote(NoteCreateDTO dto, Long userId) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setContentText(dto.getContentText());
        note.setCategoryId(dto.getCategoryId());
        note.setStatus(2); // 已发布

        // 新增：设置置顶状态（创建笔记时自动置顶）
        if (dto.getPinDuration() != null && dto.getPinDuration() >= 0) {
            note.setPinnedAt(LocalDateTime.now());
            note.setPinDuration(dto.getPinDuration());
        }

        // 新增：设置封面图
        if (dto.getCoverImage() != null) {
            note.setCoverImage(dto.getCoverImage());
        } else if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            // 自动提取第一张图作为封面
            note.setCoverImage(dto.getImages().get(0));
        }

        noteMapper.insert(note);

        // 新增：保存图片关联
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            saveNoteImages(note.getId(), dto.getImages(), dto.getCoverImage());
        }

        // 保存标签关联
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            saveNoteTags(note.getId(), dto.getTagIds());
            recordTagHistory(userId, dto.getTagIds());
        }

        // 提取关键词
        if (StringUtils.isNotBlank(dto.getContentText())) {
            saveKeywords(note.getId(), dto.getContentText());
        }

        return toNoteVO(note);
    }

    @Override
    @Transactional
    public NoteVO updateNote(Long id, NoteCreateDTO dto, Long userId) {
        Note note = getNoteWithOwnershipCheck(id, userId);

        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setContentText(dto.getContentText());
        note.setCategoryId(dto.getCategoryId());

        // 处理置顶状态
        if (dto.getPinDuration() != null) {
            if (dto.getPinDuration() >= 0) {
                // 设置置顶
                note.setPinnedAt(LocalDateTime.now());
                note.setPinDuration(dto.getPinDuration());
            } else {
                // 取消置顶（pinDuration == -1）
                note.setPinnedAt(null);
                note.setPinDuration(-1);
            }
        }

        // 新增：更新封面图
        if (dto.getCoverImage() != null) {
            note.setCoverImage(dto.getCoverImage());
        } else if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            note.setCoverImage(dto.getImages().get(0));
        } else {
            // 如果没有封面且图片列表为空，清除封面
            note.setCoverImage(null);
        }

        noteMapper.updateById(note);

        // 新增：更新图片关联
        if (dto.getImages() != null) {
            noteImageMapper.deleteByNoteId(id);
            if (!dto.getImages().isEmpty()) {
                saveNoteImages(id, dto.getImages(), dto.getCoverImage());
            }
        }

        // 更新标签关联
        if (dto.getTagIds() != null) {
            noteTagMapper.delete(new LambdaQueryWrapper<NoteTag>().eq(NoteTag::getNoteId, id));
            saveNoteTags(id, dto.getTagIds());
            recordTagHistory(userId, dto.getTagIds());
        }

        // 重新提取关键词
        if (StringUtils.isNotBlank(dto.getContentText())) {
            keywordMapper.deleteByNoteId(id);
            saveKeywords(id, dto.getContentText());
        }

        return toNoteVO(note);
    }

    // 新增：更新置顶状态
    @Override
    @Transactional
    public NoteVO updateNotePin(Long id, Integer pinDuration, Long userId) {
        Note note = getNoteWithOwnershipCheck(id, userId);
        if (pinDuration == null || pinDuration < 0) {
            // 取消置顶
            note.setPinnedAt(null);
            note.setPinDuration(-1);
        } else {
            note.setPinnedAt(LocalDateTime.now());
            note.setPinDuration(pinDuration);
        }
        noteMapper.updateById(note);
        return toNoteVO(note);
    }

    // 新增：取消置顶
    @Override
    @Transactional
    public void cancelNotePin(Long id, Long userId) {
        updateNotePin(id, -1, userId);
    }

    @Override
    @Transactional
    public NoteVO updateNoteStatus(Long id, Integer status, Long userId) {
        Note note = getNoteWithOwnershipCheck(id, userId);
        note.setStatus(status);
        noteMapper.updateById(note);
        return toNoteVO(note);
    }

    @Override
    @Transactional
    public void deleteNote(Long id, Long userId) {
        Note note = getNoteWithOwnershipCheck(id, userId);
        note.setStatus(3);
        noteMapper.updateById(note);
    }

    @Override
    @Transactional
    public void deleteNotes(List<Long> ids, Long userId) {
        for (Long id : ids) {
            try {
                deleteNote(id, userId);
            } catch (Exception e) {
                // 忽略单个删除失败，继续删除其他的
            }
        }
    }

    @Override
    @Transactional
    public void updateNoteCategory(Long id, Long categoryId, Long userId) {
        Note note = getNoteWithOwnershipCheck(id, userId);
        note.setCategoryId(categoryId);
        noteMapper.updateById(note);
    }

    @Override
    public NoteVO getNoteById(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        return toNoteVO(note);
    }

    @Override
    public IPage<NoteVO> getNoteList(Long userId, Long categoryId, Integer status, int page, int size) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        // 默认排除已删除的笔记（status=3）
        wrapper.ne(Note::getStatus, 3);
        if (userId != null) wrapper.eq(Note::getUserId, userId);
        if (categoryId != null) wrapper.eq(Note::getCategoryId, categoryId);
        if (status != null) wrapper.eq(Note::getStatus, status);

        // 排序规则：置顶优先 → 置顶时间 → 更新时间 → 创建时间
        // 使用CASE WHEN确保置顶笔记排在前面（MySQL DESC排序时NULL值默认排前面，需特殊处理）
        wrapper.last("ORDER BY CASE WHEN pinned_at IS NOT NULL AND pin_duration != -1 THEN 1 ELSE 0 END DESC, pinned_at DESC, updated_at DESC, created_at DESC");

        IPage<Note> notePage = noteMapper.selectPage(new Page<>(page, size), wrapper);
        IPage<NoteVO> voPage = notePage.convert(this::toNoteVO);
        return voPage;
    }

    @Override
    public List<KeywordVO> extractKeywords(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || StringUtils.isBlank(note.getContentText())) {
            return Collections.emptyList();
        }
        return nlpService.extractKeywords(note.getContentText(), 10);
    }

    @Override
    public List<KeywordVO> extractKeywordsFromContent(String content) {
        return nlpService.extractKeywords(content, 10);
    }

    // 新增：保存图片关联
    private void saveNoteImages(Long noteId, List<String> images, String coverImage) {
        for (int i = 0; i < images.size(); i++) {
            NoteImage noteImage = new NoteImage();
            noteImage.setNoteId(noteId);
            noteImage.setImageUrl(images.get(i));
            noteImage.setImageOrder(i);
            noteImage.setIsCover(coverImage != null && coverImage.equals(images.get(i)) ? 1 : 0);
            noteImageMapper.insert(noteImage);
        }
    }

    private void saveKeywords(Long noteId, String content) {
        List<KeywordVO> keywords = nlpService.extractKeywords(content, 10);
        for (KeywordVO kw : keywords) {
            Keyword keyword = new Keyword();
            keyword.setNoteId(noteId);
            keyword.setWord(kw.getWord());
            keyword.setWeight(kw.getWeight());
            keywordMapper.insert(keyword);
        }
    }

    private void saveNoteTags(Long noteId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            NoteTag noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tagId);
            noteTagMapper.insert(noteTag);
        }
    }

    private void recordTagHistory(Long userId, List<Long> tagIds) {
        if (userId == null || tagIds == null) return;
        for (Long tagId : tagIds) {
            // 查询是否已有记录
            LambdaQueryWrapper<UserTagHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserTagHistory::getUserId, userId);
            wrapper.eq(UserTagHistory::getTagId, tagId);
            UserTagHistory history = userTagHistoryMapper.selectOne(wrapper);

            if (history != null) {
                // 更新使用次数和时间
                history.setUseCount(history.getUseCount() + 1);
                history.setLastUsedAt(LocalDateTime.now());
                userTagHistoryMapper.updateById(history);
            } else {
                // 新增记录
                UserTagHistory newHistory = new UserTagHistory();
                newHistory.setUserId(userId);
                newHistory.setTagId(tagId);
                newHistory.setUseCount(1);
                newHistory.setLastUsedAt(LocalDateTime.now());
                userTagHistoryMapper.insert(newHistory);
            }
        }
        // 清除用户缓存以更新推荐结果
        tagService.invalidateUserCache(userId);
        tagService.invalidateRecommendationCache();
    }

    private Note getNoteWithOwnershipCheck(Long id, Long userId) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        // 如果 userId 为 null 或为默认值 1，跳过权限检查（单用户模式）
        if (userId != null && userId != 1 && !note.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此笔记");
        }
        return note;
    }

    private NoteVO toNoteVO(Note note) {
        NoteVO vo = new NoteVO();
        BeanUtils.copyProperties(note, vo);
        // 截取内容预览
        if (StringUtils.isNotBlank(note.getContentText()) && note.getContentText().length() > 200) {
            vo.setContentPreview(note.getContentText().substring(0, 200) + "...");
        } else {
            vo.setContentPreview(note.getContentText());
        }
        // 获取标签
        List<Tag> tags = tagMapper.selectByNoteId(note.getId());
        vo.setTags(tags.stream().map(Tag::getName).collect(Collectors.toList()));
        // 获取分类名称
        if (note.getCategoryId() != null) {
            Category category = categoryMapper.selectById(note.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 新增：获取图片列表
        List<NoteImage> images = noteImageMapper.selectByNoteId(note.getId());
        vo.setImages(images.stream().map(NoteImage::getImageUrl).collect(Collectors.toList()));

        // 新增：计算是否置顶
        vo.setIsPinned(isNotePinned(note));

        return vo;
    }

    // 新增：判断笔记是否当前置顶
    private boolean isNotePinned(Note note) {
        if (note.getPinnedAt() == null) return false;
        if (note.getPinDuration() == null || note.getPinDuration() < 0) return false;
        if (note.getPinDuration() == 0) return true; // 永久置顶
        // 计算是否过期
        LocalDateTime expireTime = note.getPinnedAt().plusDays(note.getPinDuration());
        return LocalDateTime.now().isBefore(expireTime);
    }
}
