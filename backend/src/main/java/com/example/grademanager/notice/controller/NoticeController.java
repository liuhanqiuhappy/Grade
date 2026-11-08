package com.example.grademanager.notice.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.identity.session.SessionUser;
import com.example.grademanager.notice.dto.FailedStudentList;
import com.example.grademanager.notice.dto.MyNoticePage;
import com.example.grademanager.notice.dto.SendNoticeRequest;
import com.example.grademanager.notice.dto.SendNoticeResult;
import com.example.grademanager.notice.dto.TeacherNoticeView;
import com.example.grademanager.notice.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 补考通知（模块五扩展）：教师生成挂科名单并发送站内信；学生查看与标记已读。 */
@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/courses/{courseId}/failed-students")
    public ApiResponse<FailedStudentList> failedStudents(@PathVariable Long courseId,
                                                         @RequestParam(required = false) Long classId,
                                                         HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(noticeService.failedStudents(courseId, classId, teacherId));
    }

    @PostMapping("/courses/{courseId}/notices")
    public ApiResponse<SendNoticeResult> send(@PathVariable Long courseId,
                                              @Valid @RequestBody SendNoticeRequest request,
                                              HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(noticeService.send(courseId, request, teacherId));
    }

    @GetMapping("/courses/{courseId}/notices")
    public ApiResponse<List<TeacherNoticeView>> list(@PathVariable Long courseId, HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(noticeService.listForTeacher(courseId, teacherId));
    }

    @PostMapping("/courses/{courseId}/notices/{noticeId}/revoke")
    public ApiResponse<Void> revoke(@PathVariable Long courseId, @PathVariable Long noticeId,
                                    HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        noticeService.revoke(courseId, noticeId, teacherId);
        return ApiResponse.ok();
    }

    @GetMapping("/my/notices")
    public ApiResponse<MyNoticePage> myNotices(HttpSession session) {
        Long userId = SessionUser.requireStudent(session).getUserId();
        return ApiResponse.ok(noticeService.myNotices(userId));
    }

    @PostMapping("/my/notices/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpSession session) {
        Long userId = SessionUser.requireStudent(session).getUserId();
        noticeService.markRead(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/my/notices/read-all")
    public ApiResponse<Void> markAllRead(HttpSession session) {
        Long userId = SessionUser.requireStudent(session).getUserId();
        noticeService.markAllRead(userId);
        return ApiResponse.ok();
    }
}
