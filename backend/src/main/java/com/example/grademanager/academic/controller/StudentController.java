package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.dto.ResetPasswordResponse;
import com.example.grademanager.academic.dto.StudentCreateRequest;
import com.example.grademanager.academic.dto.StudentCreateResponse;
import com.example.grademanager.academic.dto.StudentUpdateRequest;
import com.example.grademanager.academic.dto.StudentView;
import com.example.grademanager.academic.service.StudentService;
import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.config.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生与账号（契约模块二 §5 S1–S6）。仅允许 TEACHER，身份只从 Session 取。
 */
@RestController
@RequireRole("TEACHER")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** S1 班级学生名单 */
    @GetMapping("/api/classes/{classId}/students")
    public ApiResponse<PageResult<StudentView>> listByClass(
            @PathVariable Long classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(studentService.listByClass(classId, keyword, status, page, size));
    }

    /** S2 添加学生（建账号 + 建档案，返回一次性初始口令） */
    @PostMapping("/api/classes/{classId}/students")
    public ApiResponse<StudentCreateResponse> addStudent(@PathVariable Long classId,
                                                         @Valid @RequestBody StudentCreateRequest request) {
        return ApiResponse.ok(studentService.create(classId, request));
    }

    /** S3 学生查询（跨班检索） */
    @GetMapping("/api/students")
    public ApiResponse<PageResult<StudentView>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(studentService.search(keyword, classId, status, page, size));
    }

    /** S4 学生详情 */
    @GetMapping("/api/students/{studentId}")
    public ApiResponse<StudentView> detail(@PathVariable Long studentId) {
        return ApiResponse.ok(studentService.detail(studentId));
    }

    /** S5 修改学生（部分更新；username/password/role 等多余字段自动忽略） */
    @PatchMapping("/api/students/{studentId}")
    public ApiResponse<StudentView> update(@PathVariable Long studentId,
                                           @RequestBody StudentUpdateRequest request) {
        return ApiResponse.ok(studentService.update(studentId, request));
    }

    /** S6 重置初始口令（无请求体） */
    @PostMapping("/api/students/{studentId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long studentId) {
        return ApiResponse.ok(studentService.resetPassword(studentId));
    }
}
