package com.example.grademanager.academic.dto;

/**
 * M3A 修改课程请求（部分更新：只改传入的字段）。
 */
public class CourseUpdateRequest {

    private String courseCode;

    private String courseName;

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}
