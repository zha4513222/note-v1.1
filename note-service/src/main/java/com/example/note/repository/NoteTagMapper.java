package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.NoteTag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {
}
