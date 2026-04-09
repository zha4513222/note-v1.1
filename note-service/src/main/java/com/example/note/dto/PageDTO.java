package com.example.note.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageDTO<T> {
    private List<T> records;
    private long total;
    private long pages;
    private long current;
    private long size;
}
