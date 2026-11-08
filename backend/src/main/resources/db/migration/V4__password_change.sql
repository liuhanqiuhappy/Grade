-- V4：口令自助修改与教师重置
-- 1) 强制改密标记：新建/导入学生置 1，首次登录只放行 me / logout / 修改口令
ALTER TABLE sys_user
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=下次登录必须先修改口令' AFTER password_hash,
    ADD COLUMN password_updated_at  DATETIME   NULL COMMENT '口令最近一次变更时间（含重置）' AFTER must_change_password,
    ADD COLUMN password_reset_by    BIGINT     NULL COMMENT '最近一次被教师重置口令的操作人（sys_user.id），自助改密为 NULL' AFTER password_updated_at;

-- 2) 存量账号不批量置位：避免升级瞬间把所有既有账号锁进改密页，存量由教师按需重置
