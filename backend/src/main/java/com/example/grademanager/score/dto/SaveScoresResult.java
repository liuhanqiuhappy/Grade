package com.example.grademanager.score.dto;

import java.util.List;

/**
 * 批量保存成绩结果（H1）：
 * saved 为实际保存行数；skipped 为因所在分组已确认发布而被跳过的行数（不中断整批）；
 * skippedStudents 为被跳过学生的「姓名（学号）」描述，供前端明确提示。
 */
public record SaveScoresResult(int saved, int skipped, List<String> skippedStudents) {
}
