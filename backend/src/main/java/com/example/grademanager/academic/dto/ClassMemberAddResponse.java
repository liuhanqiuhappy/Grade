package com.example.grademanager.academic.dto;

/**
 * M3B 按班级批量加入的结果：totalCount 该班在读学生数，addedCount 新增，skippedCount 跳过（已在课程中）。
 */
public record ClassMemberAddResponse(
        Long courseId,
        Long classId,
        int totalCount,
        int addedCount,
        int skippedCount) {
}
