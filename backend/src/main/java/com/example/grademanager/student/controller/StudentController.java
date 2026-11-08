package com.example.grademanager.student.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.PageResult;
import com.example.grademanager.identity.session.SessionUser;
import com.example.grademanager.student.dto.ImportResult;
import com.example.grademanager.student.dto.StudentRequest;
import com.example.grademanager.student.dto.StudentView;
import com.example.grademanager.student.service.StudentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 学生管理（模块二）：仅教师可操作，支持 Excel 批量导入。 */
@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/classes/{classId}/students")
    public ApiResponse<PageResult<StudentView>> listByClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(studentService.listByClass(classId, page, size, keyword));
    }

    @GetMapping("/students")
    public ApiResponse<PageResult<StudentView>> search(
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(studentService.search(classId, page, size, keyword));
    }

    @PostMapping("/classes/{classId}/students")
    public ApiResponse<StudentView> create(@PathVariable Long classId,
                                           @Valid @RequestBody StudentRequest request,
                                           HttpSession session) {
        SessionUser.requireTeacher(session);
        request = new StudentRequest(request.studentNo(), request.name(), request.gender(), classId);
        return ApiResponse.ok(studentService.create(request));
    }

    @PostMapping("/classes/{classId}/students/import")
    public ApiResponse<ImportResult> importExcel(@PathVariable Long classId,
                                                 @RequestParam("file") MultipartFile file,
                                                 HttpSession session) {
        SessionUser.requireTeacher(session);
        return ApiResponse.ok(studentService.importExcel(classId, file));
    }

    @PutMapping("/students/{id}")
    public ApiResponse<StudentView> update(@PathVariable Long id,
                                           @Valid @RequestBody StudentRequest request,
                                           HttpSession session) {
        SessionUser.requireTeacher(session);
        // 修改走 /students/{id}，路径里没有 classId，换班必须由请求体给出
        if (request.classId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "classId: classId 不能为空");
        }
        return ApiResponse.ok(studentService.update(id, request));
    }

    @DeleteMapping("/students/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpSession session) {
        SessionUser.requireTeacher(session);
        studentService.delete(id);
        return ApiResponse.ok();
    }
}
