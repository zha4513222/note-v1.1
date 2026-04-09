package com.example.note.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer noteCount;
    private LocalDateTime createdAt;
}
