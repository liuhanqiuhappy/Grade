package com.example.grademanager.academic.dto;

import java.math.BigDecimal;

/** 成绩看板中的一行：score 为 null 表示留空。 */
public record GradeView(
        Long studentId,
        String studentNo,
        String name,
        String className,
        BigDecimal score,
        Integer status,
        String confirmedAt) {
}
