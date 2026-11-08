package com.example.grademanager.notice.dto;

import java.time.LocalDateTime;

/**
 * 教师端已发通知视图：readCount / totalRecipients 是「真实已读率」的唯一来源。
 * 撤回的也返回（revoked=true），保留审计痕迹。
 */
public record TeacherNoticeView(Long id, Long classId, String className,
                                String title, String content,
                                LocalDateTime examTime, String examPlace, String examScope,
                                LocalDateTime createdAt, boolean revoked,
                                int totalRecipients, int readCount, int noAccountCount) {
}
