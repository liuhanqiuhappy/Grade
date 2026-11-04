package com.example.grademanager.academic.service;

import com.example.grademanager.academic.dto.ArchiveClassResponse;
import com.example.grademanager.academic.dto.ClassCreateRequest;
import com.example.grademanager.academic.dto.ClassResponse;
import com.example.grademanager.academic.dto.ClassUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.identity.session.SessionUser;

/**
 * 班级管理（契约模块二 §5 C1–C4）。
 */
public interface ClassService {

    /** C1 班级列表：分页，keyword 模糊匹配名称，默认只返回 status=1 */
    PageResult<ClassResponse> list(String keyword, Integer gradeYear, Integer status, Integer page, Integer size);

    /** C2 新建班级（同 status=1 下同名同年级 → 2001） */
    ClassResponse create(ClassCreateRequest request, SessionUser operator);

    /** C3 重命名/改年级（部分更新；归档班拒绝 → 2002） */
    ClassResponse rename(Long classId, ClassUpdateRequest request);

    /** C4 归档（软删除；重复归档 → 2002；有未发布成绩 → 2005） */
    ArchiveClassResponse archive(Long classId);
}
