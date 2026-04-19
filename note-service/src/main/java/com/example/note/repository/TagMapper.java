package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    List<Tag> selectPopular(@Param("limit") int limit);

    List<Tag> selectByNoteId(@Param("noteId") Long noteId);

    // 统计每个标签的实际使用次数（通过 note_tag 表）
    List<Map<String, Object>> selectTagUsageStats(@Param("limit") int limit);

    // 统计已应用的标签数量（在 note_tag 表中有记录的标签）
    int selectAppliedTagCount();

    // 获取已应用的标签列表（在笔记中实际使用的标签）
    List<Tag> selectAppliedTags();
}
