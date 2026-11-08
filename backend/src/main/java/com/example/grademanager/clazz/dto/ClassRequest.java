package com.example.grademanager.clazz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassRequest(
        @NotBlank(message = "name 不能为空")
        @Size(max = 100, message = "班级名称过长") String name) {
}
