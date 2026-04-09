package com.example.note.dto;

import lombok.Data;
import java.util.List;

@Data
public class NoteCreateDTO {
    private String title;
    private String content;
    private String contentText;
    private Long categoryId;
    private List<Long> tagIds;
}
