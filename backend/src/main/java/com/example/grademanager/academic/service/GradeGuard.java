package com.example.grademanager.academic.service;

/**
 * 归档前的成绩校验位（契约 §6.3 第 3 步）。
 * 模块二阶段恒返回 false（成绩表尚不存在）；模块四只需替换本接口实现，
 * 不动 Controller 与错误码。
 */
public interface GradeGuard {

    /** 该班级是否存在未发布的成绩（模块二恒为 false） */
    boolean hasDraftGrades(Long classId);
}
