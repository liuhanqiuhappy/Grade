package com.example.grademanager.course.dto;

/** 课程视图：附带教师名、成员数与状态。 */
public record CourseView(Long id, String name, String term, Long teacherId,
                         String teacherName, String status, long memberCount) {
}
