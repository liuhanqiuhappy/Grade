package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.NotBlank;

/** M3B 单个加入课程成员：按学号定位学生。 */
public record CourseMemberAddRequest(@NotBlank(message = "studentNo 不能为空") String studentNo) {
}
