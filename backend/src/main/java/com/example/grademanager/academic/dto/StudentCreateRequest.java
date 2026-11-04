package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * S2 添加学生请求（契约 §5-S2）。不接收 password：初始口令由服务端生成。
 */
public record StudentCreateRequest(

        @NotBlank(message = "studentNo 不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "studentNo 必须为 4–32 位字母数字，不允许空格与中文")
        String studentNo,

        @NotBlank(message = "name 不能为空")
        String name) {
}
