package com.example.grademanager.student.service;

import com.example.grademanager.common.PageResult;
import com.example.grademanager.student.dto.ImportResult;
import com.example.grademanager.student.dto.ResetPasswordResult;
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

    /**
     * 教师重置单个学生口令：生成一次性临时口令，库中只存哈希并置 must_change_password=1。
     * 返回值里的明文只此一次，不落库、不打日志。重置不主动踢会话，下次登录生效。
     *
     * @param operatorUserId 发起重置的教师账号 id（写入 password_reset_by 留痕）
     */
    ResetPasswordResult resetPassword(Long studentId, Long operatorUserId);
}
