package com.example.grademanager.student.dto;

import java.util.List;

/** Excel 批量导入结果：成功条数 + 失败明细（行号与原因）。 */
public record ImportResult(int successCount, List<RowError> failures) {

    public record RowError(int row, String reason) {
    }
}
