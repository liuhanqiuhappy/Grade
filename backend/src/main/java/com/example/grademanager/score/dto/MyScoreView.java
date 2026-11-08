package com.example.grademanager.score.dto;

/**
 * 学生自助查分视图。
 * <p>published=false 表示该课程尚未最终确认，此时 score 恒为 null —— 接口层不返回任何数字（需求 §6-5b）。</p>
 */
public record MyScoreView(Long courseId, String courseName, String term,
                          String teacherName, java.math.BigDecimal score, boolean published) {
}
