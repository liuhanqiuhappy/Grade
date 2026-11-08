package com.example.grademanager.notice.service;

import com.example.grademanager.notice.dto.FailedStudentList;
import com.example.grademanager.notice.dto.MyNoticePage;
import com.example.grademanager.notice.dto.SendNoticeRequest;
import com.example.grademanager.notice.dto.SendNoticeResult;
import com.example.grademanager.notice.dto.TeacherNoticeView;

import java.util.List;

/** 补考通知：挂科名单（派生）+ 站内信发送 / 已读。 */
public interface NoticeService {

    FailedStudentList failedStudents(Long courseId, Long classId, Long teacherId);

    SendNoticeResult send(Long courseId, SendNoticeRequest request, Long teacherId);

    List<TeacherNoticeView> listForTeacher(Long courseId, Long teacherId);

    void revoke(Long courseId, Long noticeId, Long teacherId);

    MyNoticePage myNotices(Long userId);

    void markRead(Long userId, Long recipientId);

    void markAllRead(Long userId);
}
