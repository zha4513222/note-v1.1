package com.example.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.note.dto.NoteCreateDTO;
import com.example.note.dto.NoteVO;
import com.example.note.dto.KeywordVO;
import java.util.List;

public interface NoteService {

    NoteVO createNote(NoteCreateDTO dto, Long userId);

    NoteVO updateNote(Long id, NoteCreateDTO dto, Long userId);

    NoteVO updateNoteStatus(Long id, Integer status, Long userId);

    void deleteNote(Long id, Long userId);

    void deleteNotes(List<Long> ids, Long userId);

    void updateNoteCategory(Long id, Long categoryId, Long userId);

    NoteVO getNoteById(Long id);

    IPage<NoteVO> getNoteList(Long userId, Long categoryId, Integer status, int page, int size);

    List<KeywordVO> extractKeywords(Long noteId);

    List<KeywordVO> extractKeywordsFromContent(String content);
}
