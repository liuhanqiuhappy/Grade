package com.example.grademanager.score.dto;

/** 课程成绩表视图：成绩行 + 学生信息 + 版本号（并发保存时按行回传）+ 行级确认标记。 */
public record ScoreView(Long studentId, String studentNo, String studentName,
                        String className, java.math.BigDecimal score, Integer version,
                        boolean groupConfirmed) {
}
