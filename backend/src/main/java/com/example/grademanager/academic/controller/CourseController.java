package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.CourseCreateRequest;
import com.example.grademanager.academic.dto.CourseResponse;
import com.example.grademanager.academic.dto.CourseUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.service.CourseService;
import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.config.RequireRole;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程管理（模块三 M3A）。仅允许 TEACHER，身份只从 Session 取；只能管理自己的课程。
 */
@RestController
@RequestMapping("/api/courses")
@RequireRole("TEACHER")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** R1 课程列表（仅当前教师自己的课程） */
    @GetMapping
    public ApiResponse<PageResult<CourseResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(courseService.list(keyword, page, size, currentUser(httpRequest)));
    }

    /** R2 课程详情 */
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> get(@PathVariable Long courseId, HttpServletRequest httpRequest) {
        return ApiResponse.ok(courseService.get(courseId, currentUser(httpRequest)));
    }

    /** R3 新建课程 */
    @PostMapping
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CourseCreateRequest request,
                                              HttpServletRequest httpRequest) {
        return ApiResponse.ok(courseService.create(request, currentUser(httpRequest)));
    }

    /** R4 修改课程（部分更新） */
    @PatchMapping("/{courseId}")
    public ApiResponse<CourseResponse> update(@PathVariable Long courseId,
                                              @RequestBody CourseUpdateRequest request,
                                              HttpServletRequest httpRequest) {
        return ApiResponse.ok(courseService.update(courseId, request, currentUser(httpRequest)));
    }

    /** R5 归档课程（软删除） */
    @DeleteMapping("/{courseId}")
    public ApiResponse<CourseResponse> archive(@PathVariable Long courseId, HttpServletRequest httpRequest) {
        return ApiResponse.ok(courseService.archive(courseId, currentUser(httpRequest)));
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return (SessionUser) request.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
    }
}
