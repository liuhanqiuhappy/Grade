package com.example.grademanager.academic.dto;

/**
 * 课程成员视图（M3B）。name 取 sys_user.display_name。
 */
public record CourseMemberView(
        Long memberId,
        Long studentId,
        String studentNo,
        String name,
        Long classId,
        String className,
        Integer status,
        String joinedAt) {
}
