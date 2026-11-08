package com.example.grademanager.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 补考通知（一条通知发给 N 个挂科学生）。 */
@Data
@TableName("notice")
public class Notice {

    /** 一期只有补考通知；留字段便于以后扩展其他通知类型。 */
    public static final String TYPE_MAKEUP_EXAM = "MAKEUP_EXAM";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    /** 发送时的班级快照；NULL 表示全课程范围。 */
    private Long classId;

    private String className;

    private String type;

    private String title;

    private String content;

    private LocalDateTime examTime;

    private String examPlace;

    private String examScope;

    private Long createdBy;

    private LocalDateTime createdAt;

    /** 撤回后学生端不再展示，已读记录保留。 */
    private Boolean revoked;
}
