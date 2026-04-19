package com.example.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String contentText;

    private Long categoryId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer status;

    @TableField(updateStrategy = FieldStrategy.IGNORED)  // 允许更新为null
    private LocalDateTime pinnedAt;      // 置顶时间

    private Integer pinDuration;        // 置顶时长(天), 0=永久, -1=未置顶

    private String coverImage;          // 封面图URL

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
