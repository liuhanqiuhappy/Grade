package com.example.grademanager.identity.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 开发期演示账号初始化：表里没有该登录名时补一条，已存在则跳过（幂等）。
 * 口令在这里现场 BCrypt 加密，避免往 SQL 里写死哈希或明文。
 * 教师账号由种子数据/管理员创建，自助注册只产生学生账号。
 * 生产环境通过 app.demo-data.enabled=false 关闭。
 */
@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);
    private static final String DEMO_PASSWORD = "pass1234";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoAccountInitializer(UserMapper userMapper,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.demo-data.enabled:true}") boolean enabled) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        createIfAbsent("teacher01", "周老师", "TEACHER");
        createIfAbsent("student01", "刘同学", "STUDENT");
    }

    private void createIfAbsent(String username, String displayName, String role) {
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        log.warn("已创建演示账号: username={}, role={}（开发期数据，生产请关闭 app.demo-data.enabled）", username, role);
    }
}
