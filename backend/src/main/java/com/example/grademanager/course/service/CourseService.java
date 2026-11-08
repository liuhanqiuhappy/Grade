package com.example.grademanager.course.service;

import com.example.grademanager.course.dto.CourseRequest;
import com.example.grademanager.course.dto.CourseView;

import java.util.List;

/**
 * 课程管理（需求 §6-3a）：教师只能管理自己创建的课程，归属由服务端按会话中的 userId 强制校验。
 */
public interface CourseService {

    List<CourseView> list(String keyword, Long teacherId);

    CourseView create(CourseRequest request, Long teacherId);

    CourseView update(Long id, CourseRequest request, Long teacherId);

    void delete(Long id, Long teacherId);
}
