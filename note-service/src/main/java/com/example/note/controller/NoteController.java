package com.example.note.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.note.common.Result;
import com.example.note.dto.NoteCreateDTO;
import com.example.note.dto.NoteVO;
import com.example.note.dto.KeywordVO;
import com.example.note.dto.PinUpdateDTO;
import com.example.note.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public Result<IPage<NoteVO>> getNotes(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size) {
        return Result.success(noteService.getNoteList(userId, categoryId, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<NoteVO> getNote(@PathVariable Long id) {
        return Result.success(noteService.getNoteById(id));
    }

    @PostMapping
    public Result<NoteVO> createNote(@RequestBody NoteCreateDTO dto,
                                     @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.success(noteService.createNote(dto, userId));
    }

    @PutMapping("/{id}")
    public Result<NoteVO> updateNote(@PathVariable Long id,
                                     @RequestBody NoteCreateDTO dto,
                                     @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.success(noteService.updateNote(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id,
                                   @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        noteService.deleteNote(id, userId);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteNotes(@RequestBody Map<String, List<Long>> request,
                                         @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        List<Long> ids = request.get("ids");
        if (ids != null && !ids.isEmpty()) {
            noteService.deleteNotes(ids, userId);
        }
        return Result.success();
    }

    @PutMapping("/{id}/category")
    public Result<Void> updateNoteCategory(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        Long categoryId = request.get("categoryId");
        noteService.updateNoteCategory(id, categoryId, userId);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<NoteVO> publishNote(@PathVariable Long id,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.success(noteService.updateNoteStatus(id, 2, userId));
    }

    @GetMapping("/{id}/keywords")
    public Result<List<KeywordVO>> getKeywords(@PathVariable Long id) {
        return Result.success(noteService.extractKeywords(id));
    }

    @PostMapping("/extract-keywords")
    public Result<List<KeywordVO>> extractKeywords(@RequestBody Map<String, String> request) {
        return Result.success(noteService.extractKeywordsFromContent(request.get("content")));
    }

    // 新增：更新置顶状态
    @PutMapping("/{id}/pin")
    public Result<NoteVO> updateNotePin(
            @PathVariable Long id,
            @RequestBody PinUpdateDTO dto,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.success(noteService.updateNotePin(id, dto.getPinDuration(), userId));
    }

    // 新增：取消置顶
    @DeleteMapping("/{id}/pin")
    public Result<Void> cancelNotePin(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        noteService.cancelNotePin(id, userId);
        return Result.success();
    }
}
