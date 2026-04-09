package com.example.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("keyword")
public class Keyword {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String word;

    private Float weight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
