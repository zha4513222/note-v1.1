package com.example.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.note.dto.CategoryVO;
import com.example.note.entity.Category;
import com.example.note.entity.Note;
import com.example.note.repository.CategoryMapper;
import com.example.note.repository.NoteMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final NoteMapper noteMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, NoteMapper noteMapper) {
        this.categoryMapper = categoryMapper;
        this.noteMapper = noteMapper;
    }

    @Override
    public List<CategoryVO> getUserCategories(Long userId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getUserId, userId);
        wrapper.orderByAsc(Category::getSortOrder);

        List<Category> categories = categoryMapper.selectList(wrapper);
        return categories.stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    @Override
    public CategoryVO createCategory(String name, Long userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setSortOrder(0);
        categoryMapper.insert(category);
        return toCategoryVO(category);
    }

    @Override
    public void deleteCategory(Long id, Long userId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getId, id);
        wrapper.eq(Category::getUserId, userId);
        categoryMapper.delete(wrapper);
    }

    @Override
    public IPage<CategoryVO> getNotesByCategory(Long categoryId, Long userId, int page, int size) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Note::getCategoryId, categoryId);
        }
        wrapper.ne(Note::getStatus, 3); // 排除已删除
        wrapper.orderByDesc(Note::getCreatedAt);

        IPage<Note> notePage = noteMapper.selectPage(new Page<>(page, size), wrapper);

        return notePage.convert(note -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(note.getId());
            vo.setName(note.getTitle());
            return vo;
        });
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);

        // 统计该分类下的笔记数量
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getCategoryId, category.getId());
        wrapper.ne(Note::getStatus, 3);
        vo.setNoteCount(noteMapper.selectCount(wrapper).intValue());

        return vo;
    }
}
