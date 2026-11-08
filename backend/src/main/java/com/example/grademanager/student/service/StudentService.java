package com.example.grademanager.student.service;

import com.example.grademanager.common.PageResult;
import com.example.grademanager.student.dto.ImportResult;
import com.example.grademanager.student.dto.StudentRequest;
import com.example.grademanager.student.dto.StudentView;
import org.springframework.web.multipart.MultipartFile;

public interface StudentService {

    PageResult<StudentView> listByClass(Long classId, long page, long size, String keyword);

    PageResult<StudentView> search(Long classId, long page, long size, String keyword);

    StudentView create(StudentRequest request);

    StudentView update(Long id, StudentRequest request);

    void delete(Long id);

    ImportResult importExcel(Long classId, MultipartFile file);
}
