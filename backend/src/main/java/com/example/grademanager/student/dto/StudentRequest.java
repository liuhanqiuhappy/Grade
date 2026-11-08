package com.example.grademanager.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 学生新增/修改请求。
 * <p>classId 在新增时取自路径 {@code /api/classes/{classId}/students}（控制器会用它覆盖请求体），
 * 因此这里不做必填校验；修改时（{@code PUT /api/students/{id}}）需要用于换班，由控制器显式校验。
 * 曾经在这里加 @NotNull 导致「班级学生页新增学生」漏传该字段直接 4001。
 */
public record StudentRequest(
        @NotBlank(message = "studentNo 不能为空") @Size(max = 50) String studentNo,
        @NotBlank(message = "name 不能为空") @Size(max = 50) String name,
        @Size(max = 10) String gender,
        Long classId) {
}
