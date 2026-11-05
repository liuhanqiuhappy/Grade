package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.NotNull;

/** M3B 按班级批量加入课程成员。 */
public record ClassMemberAddRequest(@NotNull(message = "classId 不能为空") Long classId) {
}
