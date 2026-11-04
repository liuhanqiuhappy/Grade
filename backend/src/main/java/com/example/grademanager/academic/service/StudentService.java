package com.example.grademanager.academic.service;

import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.dto.ResetPasswordResponse;
import com.example.grademanager.academic.dto.StudentCreateRequest;
import com.example.grademanager.academic.dto.StudentCreateResponse;
import com.example.grademanager.academic.dto.StudentImportResponse;
import com.example.grademanager.academic.dto.StudentUpdateRequest;
import com.example.grademanager.academic.dto.StudentView;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学生与账号（契约模块二 §5 S1–S6）。
 */
public interface StudentService {

    /** S1 班级学生名单：允许查看已归档班级的名单（历史可查），仅不可再操作 */
    PageResult<StudentView> listByClass(Long classId, String keyword, Integer status, Integer page, Integer size);

    /** S2 添加学生：同一事务内建账号 + 建档案，返回一次性初始口令 */
    StudentCreateResponse create(Long classId, StudentCreateRequest request);

    /** S3 学生查询：跨班按学号/姓名/班级检索 */
    PageResult<StudentView> search(String keyword, Long classId, Integer status, Integer page, Integer size);

    /** S4 学生详情 */
    StudentView detail(Long studentId);

    /** S5 修改学生：改姓名/学号/转班/停用；username 不可改 */
    StudentView update(Long studentId, StudentUpdateRequest request);

    /** S6 重置初始口令：生成新 8 位口令，旧口令立即失效 */
    ResetPasswordResponse resetPassword(Long studentId);

    /** 批量导入学生（Excel，契约 v1.0 之外的扩展）：逐行独立事务，部分成功，返回成功/失败统计 */
    StudentImportResponse importFromExcel(Long classId, MultipartFile file);
}
