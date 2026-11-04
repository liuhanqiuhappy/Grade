package com.example.grademanager.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求（契约 §3.2）。
 * 登录侧只做「非空 + 长度上限」；完整格式规则只在注册时校验，
 * 避免用户的格式错误被区分成 4001，进而绕过 1001 的统一提示。
 */
public record LoginRequest(

        @NotBlank(message = "用户名不能为空")
        @Size(max = 32, message = "用户名长度不能超过 32 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不能超过 64 个字符")
        String password
) {
}
