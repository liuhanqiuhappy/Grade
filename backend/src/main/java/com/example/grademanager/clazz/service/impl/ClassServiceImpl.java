package com.example.grademanager.clazz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.grademanager.clazz.dto.ClassRequest;
import com.example.grademanager.clazz.dto.ClassView;
import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.clazz.service.ClassService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.PageResult;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ClassServiceImpl implements ClassService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SchoolClassMapper classMapper;
    private final StudentMapper studentMapper;

    public ClassServiceImpl(SchoolClassMapper classMapper, StudentMapper studentMapper) {
        this.classMapper = classMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public PageResult<ClassView> list(long page, long size, String keyword) {
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SchoolClass::getName, keyword.trim());
        }
        wrapper.orderByDesc(SchoolClass::getId);
        Page<SchoolClass> result = classMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public ClassView create(ClassRequest request) {
        requireUniqueName(null, request.name());
        SchoolClass entity = new SchoolClass();
        entity.setName(request.name().trim());
        classMapper.insert(entity);
        return toView(entity);
    }

    @Override
    public ClassView update(Long id, ClassRequest request) {
        SchoolClass entity = classMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        requireUniqueName(id, request.name());
        entity.setName(request.name().trim());
        classMapper.updateById(entity);
        return toView(entity);
    }

    @Override
    public void delete(Long id) {
        SchoolClass entity = classMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, id));
        if (studentCount > 0) {
            throw new BusinessException(ErrorCode.CLASS_NOT_EMPTY);
        }
        classMapper.deleteById(id);
    }

    private void requireUniqueName(Long excludeId, String name) {
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<SchoolClass>()
                .eq(SchoolClass::getName, name.trim());
        SchoolClass existing = classMapper.selectOne(wrapper);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.CLASS_NAME_DUP);
        }
    }

    private ClassView toView(SchoolClass entity) {
        long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, entity.getId()));
        return new ClassView(entity.getId(), entity.getName(), count,
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().format(FMT));
    }
}
