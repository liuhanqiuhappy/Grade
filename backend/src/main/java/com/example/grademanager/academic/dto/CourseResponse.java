package com.example.grademanager.academic.dto;

/**
 * M3A 课程响应。
 */
public record CourseResponse(
        Long courseId,
        String courseCode,
        String courseName,
        Integer status,
        String createdAt) {
}
