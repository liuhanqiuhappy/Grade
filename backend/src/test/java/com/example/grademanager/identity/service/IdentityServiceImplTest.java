package com.example.grademanager.identity.service;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.impl.IdentityServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class IdentityServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private IdentityServiceImpl service;
    private User teacher;

    @BeforeEach
    void setUp() {
        userMapper = Mockito.mock(UserMapper.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new IdentityServiceImpl(userMapper, passwordEncoder);

        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher01");
        teacher.setPasswordHash("$2a$10$hash");
        teacher.setDisplayName("张老师");
        teacher.setRole(User.ROLE_TEACHER);
    }

    @Test
    void loginSuccessShouldCreateSessionAndReturnUserView() {
        when(userMapper.findByUsername("teacher01")).thenReturn(teacher);
        when(passwordEncoder.matches("pass1234", "$2a$10$hash")).thenReturn(true);

        HttpSession session = new MockHttpSession();
        UserView view = service.login(new LoginRequest("teacher01", "pass1234"), session);

        assertThat(view.username()).isEqualTo("teacher01");
        assertThat(view.role()).isEqualTo(User.ROLE_TEACHER);
        assertThat(session.getAttribute("SESSION_USER")).isNotNull();
    }

    @Test
    void loginWithWrongPasswordShouldThrow1001WithUnifiedMessage() {
        when(userMapper.findByUsername("teacher01")).thenReturn(teacher);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        HttpSession session = new MockHttpSession();
        assertThatThrownBy(() -> service.login(new LoginRequest("teacher01", "bad"), session))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1001);
    }

    @Test
    void loginWithUnknownUserShouldAlsoThrow1001() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody", "x"), new MockHttpSession()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1001);
    }

    @Test
    void meWithoutSessionShouldThrow1002() {
        assertThatThrownBy(() -> service.me(new MockHttpSession()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1002);
    }

    @Test
    void logoutShouldInvalidateSession() {
        when(userMapper.findByUsername("teacher01")).thenReturn(teacher);
        when(passwordEncoder.matches("pass1234", "$2a$10$hash")).thenReturn(true);

        MockHttpSession session = new MockHttpSession();
        service.login(new LoginRequest("teacher01", "pass1234"), session);
        service.logout(session);
        assertThat(session.isInvalid()).isTrue();
    }
}
