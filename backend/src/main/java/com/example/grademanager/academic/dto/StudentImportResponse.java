package com.example.grademanager.academic.dto;

import java.util.List;

/**
 * 批量导入学生响应（本功能为契约 v1.0 之外的扩展，非 S 接口）。
 * 部分成功语义：逐行独立事务，成功行已提交；failedCount/failures 列出失败行及原因。
 */
public record StudentImportResponse(
        int totalCount,
        int successCount,
        int failedCount,
        List<Failure> failures) {

    public static StudentImportResponse of(int totalCount, int successCount, List<Failure> failures) {
        return new StudentImportResponse(totalCount, successCount, totalCount - successCount, failures);
    }

    /** 单行失败明细：row 为 Excel 行号（从 1 计数，含表头行），message 为失败原因 */
    public record Failure(int row, String studentNo, String name, String message) {
    }
}
