package com.example.grademanager.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.grademanager.identity.entity.User;

public interface UserMapper extends BaseMapper<User> {

    default User findByUsername(String username) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }
}
