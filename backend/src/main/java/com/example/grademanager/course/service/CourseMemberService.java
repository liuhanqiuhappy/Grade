package com.example.grademanager.course.service;

import com.example.grademanager.course.dto.MemberAddResult;
import com.example.grademanager.course.dto.MemberView;

import java.util.List;

public interface CourseMemberService {

    List<MemberView> listMembers(Long courseId, Long teacherId);

    /** 加入成员并快照学生当前班级（BR05）；已在课程中的跳过并在结果中报告（§6-3b）；课程确认后拒绝。 */
    MemberAddResult addMembers(Long courseId, List<Long> studentIds, Long teacherId);

    /** 移出成员；所在分组已确认发布时拒绝；同时清理其暂存成绩。 */
    void removeMember(Long courseId, Long studentId, Long teacherId);
}
