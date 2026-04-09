package com.example.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.note.dto.CategoryVO;
import com.example.note.entity.Category;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> getUserCategories(Long userId);

    CategoryVO createCategory(String name, Long userId);

    void deleteCategory(Long id, Long userId);

    IPage<CategoryVO> getNotesByCategory(Long categoryId, Long userId, int page, int size);
}
