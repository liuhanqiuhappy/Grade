package com.example.grademanager.academic.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 批量保存草稿请求。 */
public record GradeSaveRequest(@NotEmpty(message = "items 不能为空") List<GradeItemRequest> items) {
}
