package com.example.grademanager.clazz.service;

import com.example.grademanager.clazz.dto.ClassRequest;
import com.example.grademanager.clazz.dto.ClassView;
import com.example.grademanager.common.PageResult;

public interface ClassService {

    PageResult<ClassView> list(long page, long size, String keyword);

    ClassView create(ClassRequest request);

    ClassView update(Long id, ClassRequest request);

    void delete(Long id);
}
