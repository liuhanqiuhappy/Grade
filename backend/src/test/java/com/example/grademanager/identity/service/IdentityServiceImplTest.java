package com.example.grademanager.identity.service;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.RegisterRequest;
import com.example.grademanager.identity.dto.UserInfoResponse;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.impl.IdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void registerStoresHashAndForcesStudentRole() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });

        UserInfoResponse created = identityService.register(new RegisterRequest("student99", "pass1234", "李同学"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();

        assertEquals(99L, created.userId());
        assertEquals("STUDENT", saved.getRole());
        assertEquals("STUDENT", created.role());
        assertNotNull(saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("pass1234", saved.getPasswordHash()), "库中应为可校验的 BCrypt 哈希");
        assertTrue(!saved.getPasswordHash().contains("pass1234"), "库中不得出现明文口令");
    }

    @Test
    void registerDuplicateUsernameIs1004() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> identityService.register(new RegisterRequest("student01", "pass1234", "刘同学")));

        assertEquals(1004, ex.getCode());
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
