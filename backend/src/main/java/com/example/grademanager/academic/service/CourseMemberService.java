package com.example.grademanager.academic.service;

import com.example.grademanager.academic.dto.ClassMemberAddRequest;
import com.example.grademanager.academic.dto.ClassMemberAddResponse;
import com.example.grademanager.academic.dto.CourseMemberAddRequest;
import com.example.grademanager.academic.dto.CourseMemberView;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.identity.session.SessionUser;

/** 模块三 M3B：课程成员。 */
public interface CourseMemberService {

    PageResult<CourseMemberView> list(Long courseId, String keyword, Integer page, Integer size, SessionUser operator);

    CourseMemberView add(Long courseId, CourseMemberAddRequest request, SessionUser operator);

    ClassMemberAddResponse addByClass(Long courseId, ClassMemberAddRequest request, SessionUser operator);

    void remove(Long courseId, Long studentId, SessionUser operator);
}
