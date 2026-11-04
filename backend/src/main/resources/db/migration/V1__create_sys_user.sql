-- 身份模块：账号表
-- 口令只存 BCrypt 哈希（60 字符），不存明文
CREATE TABLE IF NOT EXISTS sys_user
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(32)     NOT NULL COMMENT '登录名',
    password_hash CHAR(60)        NOT NULL COMMENT 'BCrypt 口令哈希',
    display_name  VARCHAR(50)     NOT NULL COMMENT '姓名/昵称',
    role          VARCHAR(16)     NOT NULL COMMENT '角色：TEACHER / STUDENT',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    CONSTRAINT ck_sys_user_role CHECK (role IN ('TEACHER', 'STUDENT')),
    CONSTRAINT ck_sys_user_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='账号表';
