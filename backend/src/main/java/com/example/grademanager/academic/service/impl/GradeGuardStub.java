package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.entity.Grade;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.GradeMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.GradeGuard;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 成绩校验位真实实现（模块四）。
 * 模块二归档班级前调用：该班任一学生存在 status=0（草稿）的成绩，则返回 true → 2005 不可归档。
 */
@Component
public class GradeGuardStub implements GradeGuard {

    private final GradeMapper gradeMapper;
    private final StudentMapper studentMapper;

    public GradeGuardStub(GradeMapper gradeMapper, StudentMapper studentMapper) {
        this.gradeMapper = gradeMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public boolean hasDraftGrades(Long classId) {
        if (classId == null) {
            return false;
        }
        List<Long> studentIds = studentMapper.selectList(Wrappers.lambdaQuery(Student.class)
                        .eq(Student::getClassId, classId))
                .stream().map(Student::getId).toList();
        if (studentIds.isEmpty()) {
            return false;
        }
        Long count = gradeMapper.selectCount(Wrappers.lambdaQuery(Grade.class)
                .in(Grade::getStudentId, studentIds)
                .eq(Grade::getStatus, 0));
        return count != null && count > 0;
    }
}
