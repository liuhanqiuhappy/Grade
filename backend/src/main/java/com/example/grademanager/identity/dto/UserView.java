package com.example.grademanager.identity.dto;

/** 登录成功 / 会话恢复时返回的当前身份信息。 */
public record UserView(Long userId, String username, String displayName, String role) {
}
