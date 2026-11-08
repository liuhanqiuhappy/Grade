package com.example.grademanager.course.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.course.dto.CourseRequest;
import com.example.grademanager.course.dto.CourseView;
import com.example.grademanager.course.service.CourseService;
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

import java.util.List;

/** 课程管理（模块三）：仅教师可操作。 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<List<CourseView>> list(@RequestParam(required = false) String keyword,
                                              HttpSession session) {
        SessionUser teacher = SessionUser.requireTeacher(session);
        return ApiResponse.ok(courseService.list(keyword, teacher.getUserId()));
    }

    @PostMapping
    public ApiResponse<CourseView> create(@Valid @RequestBody CourseRequest request, HttpSession session) {
        SessionUser teacher = SessionUser.requireTeacher(session);
        return ApiResponse.ok(courseService.create(request, teacher.getUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseView> update(@PathVariable Long id,
                                          @Valid @RequestBody CourseRequest request,
                                          HttpSession session) {
        SessionUser teacher = SessionUser.requireTeacher(session);
        return ApiResponse.ok(courseService.update(id, request, teacher.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpSession session) {
        SessionUser teacher = SessionUser.requireTeacher(session);
        courseService.delete(id, teacher.getUserId());
        return ApiResponse.ok();
    }
}
