package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    List<Note> selectByPage(@Param("userId") Long userId,
                           @Param("categoryId") Long categoryId,
                           @Param("status") Integer status);

    List<Note> selectFullText(@Param("keyword") String keyword);

    // 按日期分组统计笔记数量（用于趋势图）
    @Select("SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM note " +
            "WHERE user_id = #{userId} " +
            "AND created_at >= #{startDate} " +
            "AND status != 3 " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> selectNotesCountByDate(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate
    );

    // 按分类分组统计笔记数量（用于饼图）
    @Select("SELECT IFNULL(category_id, 0) as categoryId, COUNT(*) as count " +
            "FROM note " +
            "WHERE user_id = #{userId} " +
            "AND status != 3 " +
            "GROUP BY IFNULL(category_id, 0)")
    List<Map<String, Object>> selectNotesCountByCategory(@Param("userId") Long userId);
}
