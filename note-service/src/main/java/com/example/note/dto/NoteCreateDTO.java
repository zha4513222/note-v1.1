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

    // 新增字段
    private Integer pinDuration;        // 置顶时长选项: 0=永久, 7=7天, 3=3天, 1=1天, null=不置顶
    private String coverImage;          // 封面图URL（可选，自动提取第一张图）
    private List<String> images;        // 笔记中图片URL列表
}
