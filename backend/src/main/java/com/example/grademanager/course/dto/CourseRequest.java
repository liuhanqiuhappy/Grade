package com.example.grademanager.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "name 不能为空") @Size(max = 100) String name,
        @NotBlank(message = "term 不能为空") @Size(max = 50) String term) {
}
