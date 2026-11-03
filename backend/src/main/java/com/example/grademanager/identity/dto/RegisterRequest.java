package com.example.grademanager.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（契约 §3.5 草案，待前后端确认）。
 * 刻意不接收 role：角色由服务端固定为 STUDENT，防止自助注册教师造成越权。
 * confirmPassword 由前端校验，后端不接收。
 */
public record RegisterRequest(

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,31}$",
                message = "用户名需以字母开头，由字母、数字、下划线组成，长度 4-32 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 个字符")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(min = 1, max = 50, message = "姓名长度需为 1-50 个字符")
        String displayName
) {
}
