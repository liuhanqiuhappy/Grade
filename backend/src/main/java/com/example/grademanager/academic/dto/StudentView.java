package com.example.grademanager.academic.dto;

/**
 * 学生对象视图（契约 §5-S1 的 StudentView，模块内多处复用，字段必须一致）。
 * name 取 sys_user.display_name；username 是登录名（建号后不变，不随学号变更）。
 */
public record StudentView(
        Long studentId,
        String studentNo,
        String name,
        String username,
        Long classId,
        String className,
        Integer classStatus,
        Integer status,
        String createdAt) {
}
