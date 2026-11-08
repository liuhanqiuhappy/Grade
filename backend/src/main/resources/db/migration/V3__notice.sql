-- V3：补考通知（站内信）——一条通知 + N 个收件人
-- 设计要点：
-- 1) 挂科名单是派生查询，不建表；只有发送动作落库（避免与 score 双写不一致）。
-- 2) 收件人一人一行：已读状态、未读数、教师端已读率才有真实数据源。
-- 3) 班级字段为发送时的快照（BR05），学生后续转班不影响历史通知归属。

CREATE TABLE notice (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT       NOT NULL,
    class_id    BIGINT       NULL COMMENT '发送时的班级快照；NULL 表示全课程范围',
    class_name  VARCHAR(100) NULL COMMENT '快照班级名（冗余，防班级改名影响历史展示）',
    type        VARCHAR(20)  NOT NULL DEFAULT 'MAKEUP_EXAM' COMMENT '一期仅 MAKEUP_EXAM',
    title       VARCHAR(100) NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    exam_time   DATETIME     NULL COMMENT '补考时间（结构化，供展示与后续提醒使用）',
    exam_place  VARCHAR(100) NULL,
    exam_scope  VARCHAR(500) NULL COMMENT '考试范围',
    created_by  BIGINT       NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '撤回后学生端不再展示，已读记录保留',
    KEY idx_notice_course (course_id),
    CONSTRAINT fk_notice_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_notice_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE notice_recipient (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id      BIGINT       NOT NULL,
    student_id     BIGINT       NOT NULL,
    user_id        BIGINT       NULL COMMENT '发送时的账号快照；NULL = 该生未绑定账号、站内信发不达',
    score_snapshot DECIMAL(5,1) NULL COMMENT '发送时的分数，留痕用于解释「为什么发给他」',
    read_at        DATETIME     NULL COMMENT '学生点击单条通知才写入，展开下拉不置已读',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notice_student (notice_id, student_id),
    KEY idx_recipient_student (student_id),
    CONSTRAINT fk_recipient_notice FOREIGN KEY (notice_id) REFERENCES notice (id),
    CONSTRAINT fk_recipient_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
