package com.example.grademanager.academic.dto;

/**
 * C4 归档班级响应（契约 §5-C4）。
 * affectedStudentCount 必须由前端在二次确认弹窗展示。
 */
public record ArchiveClassResponse(
        Long classId,
        String className,
        String archivedAt,
        long affectedStudentCount) {
}
