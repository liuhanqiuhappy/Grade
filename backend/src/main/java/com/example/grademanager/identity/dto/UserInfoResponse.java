package com.example.grademanager.identity.dto;

import com.example.grademanager.identity.entity.User;

/**
 * 身份信息响应体（契约 §3.2 / §3.3 的 data）。
 * 不含任何口令相关字段。
 */
public record UserInfoResponse(
        Long userId,
        String username,
        String displayName,
        String role
) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
