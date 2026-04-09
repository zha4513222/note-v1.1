package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    List<Tag> selectPopular(@Param("limit") int limit);

    List<Tag> selectByNoteId(@Param("noteId") Long noteId);
}
