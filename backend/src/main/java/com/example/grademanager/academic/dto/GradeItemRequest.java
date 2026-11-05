package com.example.grademanager.academic.dto;

import java.math.BigDecimal;

/** 单条成绩录入项：score 为 null 表示留空。 */
public record GradeItemRequest(Long studentId, BigDecimal score) {
}
