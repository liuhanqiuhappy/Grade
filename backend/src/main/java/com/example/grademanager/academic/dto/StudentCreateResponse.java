package com.example.grademanager.academic.dto;

/**
 * S2 添加学生响应（契约 §5-S2）。
 * initialPassword 为 8 位明文初始口令，只在此成功响应中出现一次，
 * 任何日志、任何持久化存储都不得保留。
 */
public record StudentCreateResponse(StudentView student, String initialPassword) {
}
