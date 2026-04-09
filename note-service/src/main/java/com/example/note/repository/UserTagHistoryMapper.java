package com.example.note.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.note.entity.UserTagHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTagHistoryMapper extends BaseMapper<UserTagHistory> {
}
