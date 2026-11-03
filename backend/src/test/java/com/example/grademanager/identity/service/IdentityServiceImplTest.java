package com.example.grademanager.identity.service;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserInfoResponse;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.impl.IdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private IdentityServiceImpl identityService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        identityService = new IdentityServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void loginSuccessReturnsIdentityWithoutPassword() {
        when(userMapper.selectOne(any())).thenReturn(teacher());

        UserInfoResponse user = identityService.login(new LoginRequest("teacher01", "pass1234"));

        assertEquals(1L, user.userId());
        assertEquals("teacher01", user.username());
        assertEquals("周老师", user.displayName());
        assertEquals("TEACHER", user.role());
    }

    @Test
    void wrongPasswordIs1001() {
        when(userMapper.selectOne(any())).thenReturn(teacher());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> identityService.login(new LoginRequest("teacher01", "wrong-password")));

        assertEquals(1001, ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void unknownUsernameIs1001() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> identityService.login(new LoginRequest("nobody", "pass1234")));

        assertEquals(1001, ex.getCode());
    }

    @Test
    void disabledAccountIs1001() {
        User user = teacher();
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> identityService.login(new LoginRequest("teacher01", "pass1234")));

        assertEquals(1001, ex.getCode());
    }

    private User teacher() {
        User user = new User();
        user.setId(1L);
        user.setUsername("teacher01");
        user.setDisplayName("周老师");
        user.setRole("TEACHER");
        user.setStatus(1);
        user.setPasswordHash(passwordEncoder.encode("pass1234"));
        return user;
    }
}
