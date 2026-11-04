package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * C2 新建班级请求（契约 §5-C2）。
 */
public record ClassCreateRequest(

        @NotBlank(message = "班级名称不能为空")
        String className,

        @Min(value = 2000, message = "gradeYear 必须在 2000–2100 之间")
        @Max(value = 2100, message = "gradeYear 必须在 2000–2100 之间")
        Integer gradeYear) {
}
