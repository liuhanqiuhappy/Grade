package com.example.grademanager.notice.dto;

import java.time.LocalDateTime;

/**
 * 学生端一条通知。id 为收件记录 id（不是通知 id）：标记已读时按它定位，
 * 服务端再校验该行确实属于当前登录学生。
 */
public record MyNoticeView(Long id, Long noticeId, String courseName,
                           String title, String content,
                           LocalDateTime examTime, String examPlace, String examScope,
                           LocalDateTime createdAt, boolean read) {
}
