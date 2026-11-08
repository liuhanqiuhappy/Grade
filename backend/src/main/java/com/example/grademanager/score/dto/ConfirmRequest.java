package com.example.grademanager.score.dto;

import jakarta.validation.constraints.Positive;

/**
 * 确认发布请求：body 可省略（确认全部分组）。
 * 携带 classId 时按「课程 + 班级」分组确认，其他分组不受影响。
 */
public record ConfirmRequest(@Positive Long classId) {
}
