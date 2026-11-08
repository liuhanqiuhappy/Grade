package com.example.grademanager.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通知收件人（一人一行）：已读状态与未读数都在这里，靠 notice 主表算不出来。
 * user_id 是发送时的账号快照，为空表示该生没有登录账号、站内信发不达。
 */
@Data
@TableName("notice_recipient")
public class NoticeRecipient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long studentId;

    private Long userId;

    /** 发送时的分数，留痕用于解释「为什么发给他」。 */
    private BigDecimal scoreSnapshot;

    /** 学生点击单条才写入；NULL = 未读。 */
    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
