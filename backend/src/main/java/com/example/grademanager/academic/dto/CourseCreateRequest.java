package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * M3A 新建课程请求。
 */
public record CourseCreateRequest(

        @NotBlank(message = "courseCode 不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "课程代码须为 1–32 位字母/数字/下划线/连字符")
        String courseCode,

        @NotBlank(message = "courseName 不能为空")
        String courseName) {
}
