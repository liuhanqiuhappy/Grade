package com.example.grademanager.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 发送补考通知请求。注意这里**没有学生列表**：收件人一律由服务端按挂科名单算出，
 * 允许前端传 studentIds 就等于开了「给任意学生群发」的越权口子。
 */
public record SendNoticeRequest(
        Long classId,
        @NotBlank(message = "请填写通知标题")
        @Size(max = 100, message = "通知标题不能超过 100 字")
        String title,
        @NotBlank(message = "请填写通知内容")
        @Size(max = 1000, message = "通知内容不能超过 1000 字")
        String content,
        LocalDateTime examTime,
        @Size(max = 100, message = "考试地点不能超过 100 字")
        String examPlace,
        @Size(max = 500, message = "考试范围不能超过 500 字")
        String examScope) {
}
