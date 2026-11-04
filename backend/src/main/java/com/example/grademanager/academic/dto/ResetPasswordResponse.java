package com.example.grademanager.academic.dto;

/**
 * S6 重置初始口令响应（契约 §5-S6）。initialPassword 同样只出现一次。
 */
public record ResetPasswordResponse(
        Long studentId,
        String username,
        String initialPassword,
        String resetAt) {
}
