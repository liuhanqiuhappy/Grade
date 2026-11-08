package com.example.grademanager.identity.dto;

import jakarta.validation.constraints.NotBlank;

/** 自助修改口令：仅能改本人，身份从会话取，不接受请求体里的账号/用户 id。 */
public record ChangePasswordRequest(
        @NotBlank(message = "原口令不能为空") String oldPassword,
        @NotBlank(message = "新口令不能为空") String newPassword) {
}
