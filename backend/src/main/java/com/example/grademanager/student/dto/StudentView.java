package com.example.grademanager.student.dto;

/** 学生视图：附带所属班级名。 */
public record StudentView(Long id, String studentNo, String name, String gender,
                          Long classId, String className) {
}
