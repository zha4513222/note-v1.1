package com.example.note.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteVO {
    private Long id;
    private String title;
    private String content;
    private String contentPreview;
    private Long categoryId;
    private String categoryName;
    private List<String> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 新增字段
    private LocalDateTime pinnedAt;      // 置顶时间
    private Integer pinDuration;        // 置顶时长(天)
    private Boolean isPinned;           // 是否当前置顶（计算属性）
    private String coverImage;          // 封面图URL
    private List<String> images;        // 笔记图片列表
}
