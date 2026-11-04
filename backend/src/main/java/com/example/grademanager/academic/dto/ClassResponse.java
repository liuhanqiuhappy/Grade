package com.example.grademanager.academic.dto;

/**
 * 班级对象（契约 §5-C1/C2 响应）。时间统一为 yyyy-MM-dd HH:mm:ss 字符串。
 */
public record ClassResponse(
        Long classId,
        String className,
        Integer gradeYear,
        long studentCount,
        Integer status,
        String archivedAt,
        String createdAt) {
}
