package com.example.grademanager.academic.dto;

import java.util.List;

/** 课程成绩看板：confirmed=true 表示该课程成绩已确认发布（只读）。 */
public record GradeBoardResponse(boolean confirmed, List<GradeView> list) {
}
