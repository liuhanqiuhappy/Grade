package com.example.grademanager.course.dto;

/**
 * 批量加入成员的结果（§6-3b）：
 * added = 本次实际新增人数；
 * skipped = 因已在课程中、或目标班级分组已确认发布而被跳过的人数；
 * skippedConfirmed = 其中「目标班级分组已确认发布」而被跳过的人数（M3）。
 * 已在课程中不再视为错误，避免一人重复导致整批失败。
 */
public record MemberAddResult(int added, int skipped, int skippedConfirmed) {
}
