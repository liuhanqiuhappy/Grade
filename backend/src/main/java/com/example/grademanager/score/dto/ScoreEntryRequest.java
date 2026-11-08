package com.example.grademanager.score.dto;

import java.util.List;

/** 批量保存成绩请求（仅草稿课程可保存）。 */
public record ScoreEntryRequest(@jakarta.validation.constraints.NotEmpty(message = "entries 不能为空")
                                List<ScoreEntry> entries) {
}
