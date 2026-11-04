-- 模块二：班级表（契约《模块二-班级与学生》§3.2）
-- 唯一索引含 status：归档后的同名班级不会挡住新建（名字错了归档后能重建）。
-- 代价：同一 (name, grade_year) 只能存在一条 status=0 的归档记录，
--      归档前若已存在旧归档行，服务层先物理删除该行（见 ClassServiceImpl.archive 注释）。
CREATE TABLE IF NOT EXISTS school_class
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)     NOT NULL COMMENT '班级名称（去首尾空格后 1-64 字符）',
    grade_year  SMALLINT        NULL COMMENT '年级 2000-2100，允许为空',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=正常 0=已归档',
    archived_at DATETIME        NULL COMMENT '归档时间，status=0 时非空',
    created_by  BIGINT UNSIGNED NOT NULL COMMENT '创建者 sys_user.id（仅审计，不作权限边界）',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_school_class_name (name, grade_year, status),
    KEY idx_school_class_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='班级表';
