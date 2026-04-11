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
    private final UserTagHistoryMapper userTagHistoryMapper;
    private final NLPService nlpService;
    private final TagService tagService;
    private final CategoryMapper categoryMapper;

    public NoteServiceImpl(NoteMapper noteMapper, TagMapper tagMapper,
                          KeywordMapper keywordMapper, NoteTagMapper noteTagMapper,
                          UserTagHistoryMapper userTagHistoryMapper,
                          NLPService nlpService, TagService tagService,
                          CategoryMapper categoryMapper) {
        this.noteMapper = noteMapper;
        this.tagMapper = tagMapper;
        this.keywordMapper = keywordMapper;
        this.noteTagMapper = noteTagMapper;
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
        noteMapper.insert(note);

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
        noteMapper.updateById(note);

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
        wrapper.orderByDesc(Note::getCreatedAt);

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
        return vo;
    }
}
