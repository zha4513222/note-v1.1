package com.example.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.note.common.Result;
import com.example.note.dto.NoteVO;
import com.example.note.entity.Note;
import com.example.note.entity.NoteTag;
import com.example.note.repository.NoteMapper;
import com.example.note.repository.NoteTagMapper;
import com.example.note.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final NoteService noteService;

    public SearchController(NoteMapper noteMapper, NoteTagMapper noteTagMapper, NoteService noteService) {
        this.noteMapper = noteMapper;
        this.noteTagMapper = noteTagMapper;
        this.noteService = noteService;
    }

    @GetMapping("/notes")
    public Result<List<NoteVO>> searchNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Note::getStatus, 3); // 排除已删除

        // 如果有标签过滤，先获取匹配标签的笔记ID（子查询）
        if (tagIds != null && !tagIds.isEmpty()) {
            // 使用子查询：SELECT note_id FROM note_tag WHERE tag_id IN (...)
            wrapper.inSql(Note::getId,
                "SELECT note_id FROM note_tag WHERE tag_id IN (" +
                String.join(",", tagIds.stream().map(String::valueOf).collect(Collectors.toList())) + ")");
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(Note::getTitle, keyword)
                .or()
                .like(Note::getContentText, keyword)
            );
        }

        wrapper.orderByDesc(Note::getCreatedAt);

        // 先分页查询获取笔记ID列表
        Page<Note> notePage = new Page<>(page, size);
        IPage<Note> resultPage = noteMapper.selectPage(notePage, wrapper);
        List<Note> notes = resultPage.getRecords();

        // 获取笔记VO列表
        List<NoteVO> noteVOs = new ArrayList<>();
        for (Note note : notes) {
            NoteVO vo = noteService.getNoteById(note.getId());
            noteVOs.add(vo);
        }

        return Result.success(noteVOs);
    }

    @GetMapping("/suggestions")
    public Result<List<String>> getSuggestions(@RequestParam String prefix) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Note::getTitle, prefix);
        wrapper.ne(Note::getStatus, 3);
        wrapper.select(Note::getTitle);
        wrapper.last("LIMIT 10");

        return Result.success(
            noteMapper.selectList(wrapper).stream()
                .map(Note::getTitle)
                .collect(Collectors.toList())
        );
    }
}
