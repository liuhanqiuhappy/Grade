package com.example.grademanager.notice.dto;

/** 发送结果：sent 为实际建立的收件记录数，noAccount 为其中无账号（站内信发不达）的人数。 */
public record SendNoticeResult(Long noticeId, int sent, int noAccount) {
}
