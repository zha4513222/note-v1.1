package com.example.note.controller;

import com.example.note.common.Result;
import com.example.note.dto.TagVO;
import com.example.note.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<TagVO>> getTags(@RequestParam(required = false) Long userId) {
        return Result.success(tagService.getUserTags(userId));
    }

    @GetMapping("/popular")
    public Result<List<TagVO>> getPopularTags(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(tagService.getPopularTags(limit));
    }

    @PostMapping
    public Result<TagVO> createTag(@RequestBody Map<String, String> request) {
        return Result.success(tagService.createTag(request.get("name")));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }

    @GetMapping("/recommend")
    public Result<List<TagVO>> recommendTags(
            @RequestParam Long userId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<Long> tagIds) {
        return Result.success(tagService.recommendTags(userId, content, tagIds));
    }

    /**
     * 从内容自动生成标签（最多3个）
     */
    @PostMapping("/generate")
    public Result<List<TagVO>> generateTags(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        int maxTags = request.containsKey("maxTags") ? (Integer) request.get("maxTags") : 3;
        return Result.success(tagService.generateTagsFromContent(content, maxTags));
    }

    /**
     * 获取已应用的标签列表（在笔记中实际使用的标签）
     */
    @GetMapping("/applied")
    public Result<List<TagVO>> getAppliedTags() {
        return Result.success(tagService.getAppliedTags());
    }
}
