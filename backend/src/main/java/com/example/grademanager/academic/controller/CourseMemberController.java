package com.example.grademanager.academic.controller;

import com.example.grademanager.academic.dto.ClassMemberAddRequest;
import com.example.grademanager.academic.dto.ClassMemberAddResponse;
import com.example.grademanager.academic.dto.CourseMemberAddRequest;
import com.example.grademanager.academic.dto.CourseMemberView;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.service.CourseMemberService;
import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.config.RequireRole;
import com.example.grademanager.identity.session.SessionKeys;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程成员（模块三 M3B）。仅 TEACHER，且只能操作自己的课程。
 */
@RestController
@RequestMapping("/api/courses/{courseId}/members")
@RequireRole("TEACHER")
public class CourseMemberController {

    private final CourseMemberService memberService;

    public CourseMemberController(CourseMemberService memberService) {
        this.memberService = memberService;
    }

    /** M1 课程成员列表（keyword 按学号模糊匹配） */
    @GetMapping
    public ApiResponse<PageResult<CourseMemberView>> list(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(memberService.list(courseId, keyword, page, size, currentUser(httpRequest)));
    }

    /** M2 单个加入（按学号） */
    @PostMapping
    public ApiResponse<CourseMemberView> add(@PathVariable Long courseId,
                                             @Valid @RequestBody CourseMemberAddRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.ok(memberService.add(courseId, request, currentUser(httpRequest)));
    }

    /** M3 按班级批量加入 */
    @PostMapping("/import-class")
    public ApiResponse<ClassMemberAddResponse> addByClass(@PathVariable Long courseId,
                                                          @Valid @RequestBody ClassMemberAddRequest request,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(memberService.addByClass(courseId, request, currentUser(httpRequest)));
    }

    /** M4 移出成员 */
    @DeleteMapping("/{studentId}")
    public ApiResponse<Void> remove(@PathVariable Long courseId,
                                    @PathVariable Long studentId,
                                    HttpServletRequest httpRequest) {
        memberService.remove(courseId, studentId, currentUser(httpRequest));
        return ApiResponse.ok();
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return (SessionUser) request.getAttribute(SessionKeys.CURRENT_USER_REQUEST_ATTR);
    }
}
