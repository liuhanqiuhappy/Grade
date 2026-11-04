package com.example.grademanager.academic.service.impl;

import com.example.grademanager.academic.service.GradeGuard;
import org.springframework.stereotype.Component;

/**
 * 模块二占位实现：成绩表尚不存在，恒返回 false（契约 §6.3 / §4.2-2005）。
 * 模块四实现成绩功能时替换为本模块的真实实现。
 */
@Component
public class GradeGuardStub implements GradeGuard {

    @Override
    public boolean hasDraftGrades(Long classId) {
        return false;
    }
}
