package com.example.note.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.note.common.Result;
import com.example.note.dto.CategoryVO;
import com.example.note.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<CategoryVO>> getCategories(
            @RequestParam(required = false, defaultValue = "1") Long userId) {
        return Result.success(categoryService.getUserCategories(userId));
    }

    @PostMapping
    public Result<CategoryVO> createCategory(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.success(categoryService.createCategory(request.get("name"), userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        categoryService.deleteCategory(id, userId);
        return Result.success();
    }

    @GetMapping("/{id}/notes")
    public Result<IPage<CategoryVO>> getNotesByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size) {
        return Result.success(categoryService.getNotesByCategory(id, userId, page, size));
    }
}
