-- 模块三 M3A：课程表
-- 教师只能管理自己创建的课程（teacher_id 归属校验在服务层完成）
CREATE TABLE IF NOT EXISTS course
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，即接口里的 courseId',
    course_code VARCHAR(32)     NOT NULL COMMENT '课程代码（业务标识，全局唯一）',
    course_name VARCHAR(64)     NOT NULL COMMENT '课程名称',
    teacher_id  BIGINT UNSIGNED NOT NULL COMMENT '负责教师 sys_user.id；仅本人可管理',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=正常 0=已归档',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_course_teacher_id (teacher_id),
    CONSTRAINT ck_course_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='课程表（M3A 课程管理）';
