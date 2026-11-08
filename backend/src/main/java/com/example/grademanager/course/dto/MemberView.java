package com.example.grademanager.course.dto;

/** 课程成员视图：学号/姓名 + 加入时班级快照 + 分组确认状态。 */
public record MemberView(Long studentId, String studentNo, String studentName,
                         String className, Long classId, boolean groupConfirmed) {
}
