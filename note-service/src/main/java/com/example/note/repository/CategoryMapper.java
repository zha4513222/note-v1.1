package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
