package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.Keyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface KeywordMapper extends BaseMapper<Keyword> {

    List<Keyword> selectByNoteId(@Param("noteId") Long noteId);

    void deleteByNoteId(@Param("noteId") Long noteId);
}
