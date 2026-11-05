package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.GradeBoardResponse;
import com.example.grademanager.academic.dto.GradeSaveRequest;
import com.example.grademanager.academic.service.GradeService;
import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.config.RequireRole;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成绩录入与确认（模块四）。仅 TEACHER，且只能操作自己的课程。
 */
@RestController
@RequestMapping("/api/courses/{courseId}/grades")
@RequireRole("TEACHER")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    /** G1 成绩看板（课程成员 + 分数 + 确认状态） */
    @GetMapping
    public ApiResponse<GradeBoardResponse> board(@PathVariable Long courseId, HttpServletRequest httpRequest) {
        return ApiResponse.ok(gradeService.board(courseId, currentUser(httpRequest)));
    }

    /** G2 批量保存草稿（允许留空） */
    @PutMapping
    public ApiResponse<Integer> save(@PathVariable Long courseId,
                                     @Valid @RequestBody GradeSaveRequest request,
                                     HttpServletRequest httpRequest) {
        return ApiResponse.ok(gradeService.save(courseId, request, currentUser(httpRequest)));
    }

    /** G3 确认发布（之后只读锁定） */
    @PostMapping("/confirm")
    public ApiResponse<GradeBoardResponse> confirm(@PathVariable Long courseId, HttpServletRequest httpRequest) {
        return ApiResponse.ok(gradeService.confirm(courseId, currentUser(httpRequest)));
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return (SessionUser) request.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
    }
}
