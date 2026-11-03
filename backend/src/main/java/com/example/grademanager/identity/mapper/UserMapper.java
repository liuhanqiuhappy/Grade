package com.example.grademanager.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.grademanager.identity.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
