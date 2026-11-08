package com.example.grademanager.notice.dto;

import java.math.BigDecimal;

/**
 * 挂科名单一行。hasAccount=false 表示该生未绑定登录账号，站内信发不达——
 * 必须显式暴露给前端标出来，不能静默丢弃（否则教师以为通知到位了）。
 */
public record FailedStudentView(Long studentId, String studentNo, String studentName,
                                String className, BigDecimal score,
                                boolean hasAccount, boolean notified) {
}
