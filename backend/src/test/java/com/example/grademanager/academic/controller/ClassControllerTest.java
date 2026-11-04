package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.ClassResponse;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.service.ClassService;
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

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 班级接口层验证（覆盖契约 §10.1 的 T-SEC-01/T-SEC-02 及 C 系列状态码映射）。
 * standalone MockMvc + 拦截器顺序：Session → Role（与 WebMvcConfig 一致）。
 */
class ClassControllerTest {

    private ClassService classService;
    private MockMvc mockMvc;

    private final MockHttpSession teacherSession = session("TEACHER");
    private final MockHttpSession studentSession = session("STUDENT");

    @BeforeEach
    void setUp() {
        classService = mock(ClassService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ClassController(classService))
                .setControllerAdvice(new GlobalExceptionHandler())
                // 顺序必须 Session 在前、Role 在后（契约 §7.4）
                .addInterceptors(new SessionAuthInterceptor(), new RoleAuthInterceptor())
                .build();
    }

    @Test
    void unauthenticatedIs1002Not1003() throws Exception {
        // T-SEC-02：未登录调用 → 1002（不是 1003，验证拦截器顺序）
        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void studentRoleIs1003() throws Exception {
        // T-SEC-01：学生身份调用 C1 → 1003
        mockMvc.perform(get("/api/classes").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void teacherListReturnsPagedData() throws Exception {
        when(classService.list(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PageResult<>(List.of(
                        new ClassResponse(3L, "计算机2301班", 2023, 42, 1, null, "2026-11-03 20:11:02")),
                        1, 1, 10));

        mockMvc.perform(get("/api/classes").session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].classId").value(3))
                .andExpect(jsonPath("$.data.list[0].className").value("计算机2301班"))
                .andExpect(jsonPath("$.data.list[0].studentCount").value(42))
                .andExpect(jsonPath("$.data.list[0].archivedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void createMissingClassNameIs4001() throws Exception {
        mockMvc.perform(post("/api/classes").session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("班级名称不能为空"));
    }

    @Test
    void createWhitespaceNameIs4001() throws Exception {
        mockMvc.perform(post("/api/classes").session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"   \",\"gradeYear\":2023}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void createDuplicateMaps2001() throws Exception {
        when(classService.create(any(), any())).thenThrow(
                new BusinessException(ErrorCode.CLASS_NAME_DUPLICATED, "班级「计算机2301班」已存在"));

        mockMvc.perform(post("/api/classes").session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"计算机2301班\",\"gradeYear\":2023}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.message").value("班级「计算机2301班」已存在"));
    }

    @Test
    void renameNothingProvidedMaps4001() throws Exception {
        when(classService.rename(any(), any())).thenThrow(
                new BusinessException(ErrorCode.PARAM_INVALID, "未提供任何可改字段"));

        mockMvc.perform(patch("/api/classes/3").session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void archiveTwiceMaps2002() throws Exception {
        when(classService.archive(any())).thenThrow(new BusinessException(ErrorCode.CLASS_NOT_FOUND));

        mockMvc.perform(delete("/api/classes/3").session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void invalidPageParamIs4001() throws Exception {
        when(classService.list(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.PARAM_INVALID, "page 必须 ≥1"));

        mockMvc.perform(get("/api/classes?page=0").session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void nonNumericPathIdIs4001() throws Exception {
        mockMvc.perform(delete("/api/classes/abc").session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    private MockHttpSession session(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER,
                new SessionUser(1L, "teacher01", "周老师", role));
        return session;
    }
}
