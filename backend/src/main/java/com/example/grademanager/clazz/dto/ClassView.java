package com.example.grademanager.clazz.dto;

/** 班级视图：附带学生数，便于列表展示。 */
public record ClassView(Long id, String name, long studentCount, String createdAt) {
}
