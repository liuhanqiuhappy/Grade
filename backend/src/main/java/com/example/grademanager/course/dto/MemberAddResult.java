package com.example.grademanager.course.dto;

/**
 * 批量加入成员的结果（§6-3b）：
 * added = 本次实际新增人数，skipped = 因已在课程中而跳过的人数。
 * 已在课程中不再视为错误，避免一人重复导致整批失败。
 */
public record MemberAddResult(int added, int skipped) {
}
