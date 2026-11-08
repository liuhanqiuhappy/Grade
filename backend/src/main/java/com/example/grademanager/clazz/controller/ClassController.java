package com.example.grademanager.clazz.controller;

import com.example.grademanager.clazz.dto.ClassRequest;
import com.example.grademanager.clazz.dto.ClassView;
import com.example.grademanager.clazz.service.ClassService;
import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.common.PageResult;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 班级管理（模块二）：仅教师可操作。 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ApiResponse<PageResult<ClassView>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(classService.list(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<ClassView> create(@Valid @RequestBody ClassRequest request, HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(classService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClassView> update(@PathVariable Long id,
                                         @Valid @RequestBody ClassRequest request,
                                         HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(classService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpSession session) {
        SessionUser.requireTeacher(session);
        classService.delete(id);
        return ApiResponse.ok();
    }
}
