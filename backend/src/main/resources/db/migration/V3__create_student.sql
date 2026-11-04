-- 模块二：学生档案表（契约《模块二-班级与学生》§3.3）
-- 姓名只存 sys_user.display_name，本表不冗余 name 字段（查询时 JOIN 取）。
CREATE TABLE IF NOT EXISTS student
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，即接口里的 studentId',
    user_id    BIGINT UNSIGNED NOT NULL COMMENT 'sys_user.id，1:1，唯一',
    student_no VARCHAR(32)     NOT NULL COMMENT '学号（业务标识，可修改；登录名建号后不变）',
    class_id   BIGINT UNSIGNED NOT NULL COMMENT 'school_class.id，学生必须有班',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1=在读 0=停用（停用后不能登录、不能加课）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user_id (user_id),
    UNIQUE KEY uk_student_student_no (student_no),
    KEY idx_student_class_id (class_id),
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES school_class (id),
    CONSTRAINT ck_student_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='学生档案表';
