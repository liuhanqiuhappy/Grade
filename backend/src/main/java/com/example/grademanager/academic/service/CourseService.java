package com.example.grademanager.academic.service;

import com.example.grademanager.academic.dto.CourseCreateRequest;
import com.example.grademanager.academic.dto.CourseResponse;
import com.example.grademanager.academic.dto.CourseUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.identity.session.SessionUser;

/**
 * 课程管理（模块三 M3A）：教师只能管理自己创建的课程。
 */
public interface CourseService {

    /** 课程列表（仅返回当前教师的课程） */
    PageResult<CourseResponse> list(String keyword, Integer page, Integer size, SessionUser operator);

    /** 课程详情（归属校验） */
    CourseResponse get(Long courseId, SessionUser operator);

    /** 新建课程（teacher_id = 当前教师） */
    CourseResponse create(CourseCreateRequest request, SessionUser operator);

    /** 修改课程代码/名称（归属校验 + 部分更新） */
    CourseResponse update(Long courseId, CourseUpdateRequest request, SessionUser operator);

    /** 归档课程（软删除，归属校验） */
    CourseResponse archive(Long courseId, SessionUser operator);
}
