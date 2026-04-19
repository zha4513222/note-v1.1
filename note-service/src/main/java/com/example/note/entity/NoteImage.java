package com.example.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note_image")
public class NoteImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String imageUrl;

    private Integer imageOrder;       // 图片顺序

    private Integer isCover;          // 是否为封面图: 0-否, 1-是

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}