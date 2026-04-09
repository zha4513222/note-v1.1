package com.example.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.note.common.Result;
import com.example.note.entity.Note;
import com.example.note.entity.Tag;
import com.example.note.repository.NoteMapper;
import com.example.note.repository.TagMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final NoteMapper noteMapper;
    private final TagMapper tagMapper;

    public StatsController(NoteMapper noteMapper, TagMapper tagMapper) {
        this.noteMapper = noteMapper;
        this.tagMapper = tagMapper;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(@RequestParam Long userId) {
        Map<String, Object> overview = new HashMap<>();

        // 笔记总数
        LambdaQueryWrapper<Note> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(Note::getUserId, userId);
        noteWrapper.ne(Note::getStatus, 3);
        long totalNotes = noteMapper.selectCount(noteWrapper);
        overview.put("totalNotes", totalNotes);

        // 已发布笔记数
        LambdaQueryWrapper<Note> publishedWrapper = new LambdaQueryWrapper<>();
        publishedWrapper.eq(Note::getUserId, userId);
        publishedWrapper.eq(Note::getStatus, 2);
        long publishedNotes = noteMapper.selectCount(publishedWrapper);
        overview.put("publishedNotes", publishedNotes);

        // 标签总数
        long totalTags = tagMapper.selectCount(null);
        overview.put("totalTags", totalTags);

        // 草稿数
        long draftNotes = totalNotes - publishedNotes;
        overview.put("draftNotes", draftNotes);

        return Result.success(overview);
    }

    @GetMapping("/notes/trend")
    public Result<List<Map<String, Object>>> getNotesTrend(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days) {

        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.minusDays(days - 1).atStartOfDay();

        // 单次查询获取所有日期的统计
        List<Map<String, Object>> rawData = noteMapper.selectNotesCountByDate(userId, startDate);

        // 转换为以日期为key的map，便于补全缺失日期
        Map<String, Long> countByDate = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            Object dateObj = row.get("date");
            Object countObj = row.get("count");
            if (dateObj != null && countObj != null) {
                countByDate.put(dateObj.toString(), ((Number) countObj).longValue());
            }
        }

        // 构建完整日期序列（补全缺失日期为0）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            long count = countByDate.getOrDefault(dateStr, 0L);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("count", count);
            trend.add(dayData);
        }

        return Result.success(trend);
    }

    @GetMapping("/tags/usage")
    public Result<List<Map<String, Object>>> getTagsUsage(@RequestParam Long userId) {
        List<Tag> tags = tagMapper.selectPopular(10);

        return Result.success(tags.stream().map(tag -> {
            Map<String, Object> tagData = new HashMap<>();
            tagData.put("name", tag.getName());
            tagData.put("count", tag.getUseCount());
            return tagData;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/notes/category-dist")
    public Result<List<Map<String, Object>>> getCategoryDistribution(@RequestParam Long userId) {
        // 使用SQL聚合查询按分类统计笔记数量
        List<Map<String, Object>> results = noteMapper.selectNotesCountByCategory(userId);

        return Result.success(results.stream().map(row -> {
            Map<String, Object> catData = new HashMap<>();
            catData.put("categoryId", row.get("categoryId"));
            catData.put("count", row.get("count"));
            return catData;
        }).collect(Collectors.toList()));
    }
}
