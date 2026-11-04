package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.ArchiveClassResponse;
import com.example.grademanager.academic.dto.ClassCreateRequest;
import com.example.grademanager.academic.dto.ClassResponse;
import com.example.grademanager.academic.dto.ClassUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.service.ClassService;
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
 * 班级管理（契约模块二 §5 C1–C4）。仅允许 TEACHER，身份只从 Session 取。
 */
@RestController
@RequestMapping("/api/classes")
@RequireRole("TEACHER")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /** C1 班级列表 */
    @GetMapping
    public ApiResponse<PageResult<ClassResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gradeYear,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(classService.list(keyword, gradeYear, status, page, size));
    }

    /** C2 新建班级 */
    @PostMapping
    public ApiResponse<ClassResponse> create(@Valid @RequestBody ClassCreateRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.ok(classService.create(request, currentUser(httpRequest)));
    }

    /** C3 重命名班级（部分更新） */
    @PatchMapping("/{classId}")
    public ApiResponse<ClassResponse> rename(@PathVariable Long classId,
                                             @Valid @RequestBody ClassUpdateRequest request) {
        return ApiResponse.ok(classService.rename(classId, request));
    }

    /** C4 归档班级（软删除，绝不物理删除学生与成绩数据） */
    @DeleteMapping("/{classId}")
    public ApiResponse<ArchiveClassResponse> archive(@PathVariable Long classId) {
        return ApiResponse.ok(classService.archive(classId));
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return (SessionUser) request.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
    }
}
