package com.example.grademanager.score.dto;

import java.util.List;

/** 成绩 Excel 导入结果：成功条数 + 失败明细（Excel 行号与原因）。失败行不中断整批。 */
public record ScoreImportResult(int successCount, List<RowError> failures) {

    /** row 为 Excel 中的实际行号（从 1 开始，含表头行）。 */
    public record RowError(int row, String reason) {
    }
}
