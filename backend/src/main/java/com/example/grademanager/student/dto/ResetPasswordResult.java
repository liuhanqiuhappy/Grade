package com.example.grademanager.student.dto;

/**
 * 教师重置学生口令的结果。
 * temporaryPassword 是唯一一次明文返回：库里只存哈希，接口不再提供二次查询。
 */
public record ResetPasswordResult(Long studentId, String studentNo, String studentName,
                                  String temporaryPassword) {
}
