package com.example.grademanager.identity.service;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.dto.ChangePasswordRequest;
import com.example.grademanager.identity.dto.LoginRequest;
import com.example.grademanager.identity.dto.UserView;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.impl.IdentityServiceImpl;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    /* ============ 自助改口令（M5） ============ */

    @Test
    void changePasswordShouldUpdateHashAndClearMustChangeFlagInSession() {
        when(userMapper.selectById(1L)).thenReturn(teacher);
        when(passwordEncoder.matches("pass1234", "$2a$10$hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$new");

        MockHttpSession session = loggedInSession();
        service.changePassword(new ChangePasswordRequest("pass1234", "newpass123"), session);

        assertThat(teacher.getPasswordHash()).isEqualTo("$2a$10$new");
        assertThat(teacher.getMustChangePassword()).isFalse();
        assertThat(teacher.getPasswordUpdatedAt()).isNotNull();
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        assertThat(current.isMustChangePassword()).isFalse();
        verify(userMapper).updateById(teacher);
    }

    @Test
    void changePasswordWithWrongOldPasswordShouldThrow1001AndKeepHash() {
        when(userMapper.selectById(1L)).thenReturn(teacher);
        when(passwordEncoder.matches("bad", "$2a$10$hash")).thenReturn(false);

        MockHttpSession session = loggedInSession();
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("bad", "newpass123"), session))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(1001));
        assertThat(teacher.getPasswordHash()).isEqualTo("$2a$10$hash");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void changePasswordWithWeakNewPasswordShouldThrow4001() {
        when(userMapper.selectById(1L)).thenReturn(teacher);
        when(passwordEncoder.matches("pass1234", "$2a$10$hash")).thenReturn(true);

        MockHttpSession session = loggedInSession();
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("pass1234", "12345678"), session))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(4001));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void changePasswordWithoutSessionShouldThrow1002() {
        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordRequest("pass1234", "newpass123"), new MockHttpSession()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(1002));
    }

    @Test
    void loginShouldCarryMustChangePasswordFlagToSessionAndView() {
        teacher.setMustChangePassword(true);
        when(userMapper.findByUsername("teacher01")).thenReturn(teacher);
        when(passwordEncoder.matches("pass1234", "$2a$10$hash")).thenReturn(true);

        MockHttpSession session = new MockHttpSession();
        UserView view = service.login(new LoginRequest("teacher01", "pass1234"), session);

        assertThat(view.mustChangePassword()).isTrue();
        assertThat(((SessionUser) session.getAttribute(SessionUser.SESSION_KEY)).isMustChangePassword()).isTrue();
    }

    private MockHttpSession loggedInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.SESSION_KEY, SessionUser.of(teacher));
        return session;
    }
}
