package com.example.grademanager.identity.controller;

import com.example.grademanager.common.GlobalExceptionHandler;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.impl.IdentityServiceImpl;
import com.example.grademanager.identity.session.SessionAuthInterceptor;
import com.example.grademanager.identity.session.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层验证：HTTP 一律 200、业务结果看 code、会话流转、参数校验 4001。
 * 用 standalone MockMvc + mock 的 Mapper，不依赖 MySQL，便于随时回归。
 */
class IdentityControllerTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();

        IdentityController controller = new IdentityController(
                new IdentityServiceImpl(userMapper, passwordEncoder));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                // 只对受保护接口挂登录拦截器，与 WebMvcConfig 的排除清单保持一致
                .addMappedInterceptors(new String[]{"/api/identity/me"}, new SessionAuthInterceptor())
                .build();
    }

    @Test
    void loginSuccessReturns200WithIdentityAndSession() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(teacher());

        MvcResult result = mockMvc.perform(post("/api/identity/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"teacher01\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("teacher01"))
                .andExpect(jsonPath("$.data.displayName").value("周老师"))
                .andExpect(jsonPath("$.data.role").value("TEACHER"))
                // 响应体不得出现任何口令字段
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        assertNotNull(result.getRequest().getSession(false), "登录成功应建立会话");
        assertNotNull(result.getRequest().getSession(false).getAttribute(SessionKeys.CURRENT_USER));
    }

    @Test
    void loginMissingFieldIs4001WithHttp200() throws Exception {
        mockMvc.perform(post("/api/identity/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"teacher01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("密码不能为空"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void loginWrongPasswordIs1001WithHttp200() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(teacher());

        mockMvc.perform(post("/api/identity/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"teacher01\",\"password\":\"bad-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meWithoutSessionIs1002() throws Exception {
        mockMvc.perform(get("/api/identity/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void meWithSessionReturnsIdentity() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER,
                new com.example.grademanager.identity.session.SessionUser(2L, "student01", "刘同学", "STUDENT"));

        mockMvc.perform(get("/api/identity/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("student01"))
                .andExpect(jsonPath("$.data.displayName").value("刘同学"));
    }

    @Test
    void logoutIsIdempotentAndInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER,
                new com.example.grademanager.identity.session.SessionUser(2L, "student01", "刘同学", "STUDENT"));

        mockMvc.perform(post("/api/identity/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertTrue(session.isInvalid(), "退出应使会话失效");

        // 已失效的会话再次退出仍返回 code=0（幂等）
        mockMvc.perform(post("/api/identity/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void registerSuccessReturnsStudentRoleWithoutPassword() throws Exception {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });

        mockMvc.perform(post("/api/identity/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"student99\",\"password\":\"pass1234\",\"displayName\":\"李同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("student99"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registerInvalidUsernameIs4001() throws Exception {
        mockMvc.perform(post("/api/identity/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"1abc\",\"password\":\"pass1234\",\"displayName\":\"李同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("用户名需以字母开头，由字母、数字、下划线组成，长度 4-32 个字符"));
    }

    @Test
    void registerShortPasswordIs4001() throws Exception {
        mockMvc.perform(post("/api/identity/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"student99\",\"password\":\"1234\",\"displayName\":\"李同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("密码长度需为 8-64 个字符"));
    }

    @Test
    void registerDuplicateUsernameIs1004() throws Exception {
        when(userMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(post("/api/identity/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"pass1234\",\"displayName\":\"刘同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }

    @Test
    void malformedJsonIs4001() throws Exception {
        mockMvc.perform(post("/api/identity/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\": "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
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
