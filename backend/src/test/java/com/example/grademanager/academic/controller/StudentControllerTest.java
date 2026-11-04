package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.ResetPasswordResponse;
import com.example.grademanager.academic.dto.StudentCreateRequest;
import com.example.grademanager.academic.dto.StudentCreateResponse;
import com.example.grademanager.academic.dto.StudentView;
import com.example.grademanager.academic.service.StudentService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.GlobalExceptionHandler;
import com.example.grademanager.config.RoleAuthInterceptor;
import com.example.grademanager.identity.session.SessionAuthInterceptor;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 学生接口层验证（契约模块二 §5 S 系列 code 映射与权限）。
 */
class StudentControllerTest {

    private StudentService studentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        studentService = mock(StudentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentController(studentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new SessionAuthInterceptor(), new RoleAuthInterceptor())
                .build();
    }

    @Test
    void studentRoleIs1003OnAllStudentEndpoints() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER,
                new SessionUser(2L, "student01", "刘同学", "STUDENT"));

        mockMvc.perform(get("/api/classes/3/students").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
        mockMvc.perform(get("/api/students").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void unauthenticatedIs1002() throws Exception {
        mockMvc.perform(get("/api/students/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void addStudentOkReturnsViewWithOneTimePassword() throws Exception {
        StudentView view = new StudentView(10L, "2023010101", "刘同学", "2023010101",
                3L, "计算机2301班", 1, 1, "2026-11-03 21:02:33");
        when(studentService.create(eq(3L), any(StudentCreateRequest.class)))
                .thenReturn(new StudentCreateResponse(view, "K7mQ2xRt"));

        mockMvc.perform(post("/api/classes/3/students").session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2023010101\",\"name\":\"刘同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.student.studentId").value(10))
                .andExpect(jsonPath("$.data.student.username").value("2023010101"))
                .andExpect(jsonPath("$.data.initialPassword").value("K7mQ2xRt"))
                // 响应里绝不允许出现口令哈希
                .andExpect(jsonPath("$.data.student.passwordHash").doesNotExist());
    }

    @Test
    void addStudentMissingStudentNoIs4001() throws Exception {
        mockMvc.perform(post("/api/classes/3/students").session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"刘同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("studentNo 不能为空"));
    }

    @Test
    void addStudentInvalidStudentNoIs4001() throws Exception {
        mockMvc.perform(post("/api/classes/3/students").session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2023 0101\",\"name\":\"刘同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void addStudentServiceErrorsAreMapped() throws Exception {
        when(studentService.create(eq(3L), any(StudentCreateRequest.class))).thenThrow(
                new BusinessException(ErrorCode.TARGET_CLASS_ARCHIVED));

        mockMvc.perform(post("/api/classes/3/students").session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2023010101\",\"name\":\"刘同学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    void updateIgnoresUnknownFields() throws Exception {
        StudentView view = new StudentView(10L, "2023999999", "刘一鸣", "2023010101",
                3L, "计算机2301班", 1, 1, "2026-11-03 21:02:33");
        when(studentService.update(eq(10L), any())).thenReturn(view);

        // 契约 §5-S5：契约外字段（username/password）服务端忽略、不报错、改不动
        mockMvc.perform(patch("/api/students/10").session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"刘一鸣\",\"username\":\"hacked\",\"password\":\"x\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("刘一鸣"))
                .andExpect(jsonPath("$.data.username").value("2023010101"));
    }

    @Test
    void resetPasswordOkReturnsOneTimePassword() throws Exception {
        when(studentService.resetPassword(10L)).thenReturn(
                new ResetPasswordResponse(10L, "2023010101", "T4nR8wYc", "2026-11-03 22:40:12"));

        mockMvc.perform(post("/api/students/10/reset-password").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.initialPassword").value("T4nR8wYc"))
                .andExpect(jsonPath("$.data.username").value("2023010101"));
    }

    @Test
    void detailNotFoundMaps2004() throws Exception {
        when(studentService.detail(99L)).thenThrow(new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        mockMvc.perform(get("/api/students/99").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2004));
    }

    private MockHttpSession teacherSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER,
                new SessionUser(1L, "teacher01", "周老师", "TEACHER"));
        return session;
    }
}
