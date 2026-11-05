package com.example.grademanager.academic.service;

import com.example.grademanager.academic.dto.GradeBoardResponse;
import com.example.grademanager.academic.dto.GradeSaveRequest;
import com.example.grademanager.identity.session.SessionUser;

/** 模块四：成绩录入与确认。 */
public interface GradeService {

    GradeBoardResponse board(Long courseId, SessionUser operator);

    int save(Long courseId, GradeSaveRequest request, SessionUser operator);

    GradeBoardResponse confirm(Long courseId, SessionUser operator);
}
