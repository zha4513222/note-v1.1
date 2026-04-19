package com.example.note.dto;

import lombok.Data;

@Data
public class PinUpdateDTO {
    private Integer pinDuration;    // 0=永久, 7=7天, 3=3天, 1=1天, null=取消置顶
}