package com.example.grademanager.course.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.course.dto.MemberAddResult;
import com.example.grademanager.course.dto.MemberRequest;
import com.example.grademanager.course.dto.MemberView;
import com.example.grademanager.course.service.CourseMemberService;
import com.example.grademanager.identity.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 课程成员管理（模块三 B）：仅教师可操作。 */
@RestController
@RequestMapping("/api/courses/{courseId}/members")
public class CourseMemberController {

    private final CourseMemberService memberService;

    public CourseMemberController(CourseMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<List<MemberView>> list(@PathVariable Long courseId, HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(memberService.listMembers(courseId, teacherId));
    }

    @PostMapping
    public ApiResponse<Map<String, Integer>> add(@PathVariable Long courseId,
                                                 @Valid @RequestBody MemberRequest request,
                                                 HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        MemberAddResult result = memberService.addMembers(courseId, request.studentIds(), teacherId);
        return ApiResponse.ok(Map.of("added", result.added(), "skipped", result.skipped()));
    }

    @DeleteMapping("/{studentId}")
    public ApiResponse<Void> remove(@PathVariable Long courseId,
                                    @PathVariable Long studentId,
                                    HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        memberService.removeMember(courseId, studentId, teacherId);
        return ApiResponse.ok();
    }
}
