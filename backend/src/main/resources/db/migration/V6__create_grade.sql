-- 模块四：成绩表（课程内按学生录分）
-- status: 0=草稿（可改） 1=已确认发布（只读锁定）
CREATE TABLE IF NOT EXISTS grade
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id    BIGINT UNSIGNED NOT NULL COMMENT 'course.id',
    student_id   BIGINT UNSIGNED NOT NULL COMMENT 'student.id',
    score        DECIMAL(5, 2)   NULL COMMENT '分数 0-100，NULL 表示留空未录入',
    status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已确认发布',
    version      INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    confirmed_at DATETIME        NULL COMMENT '确认发布时间',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grade_course_student (course_id, student_id),
    KEY idx_grade_student_id (student_id),
    KEY idx_grade_status (status),
    CONSTRAINT ck_grade_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_grade_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='成绩表（模块四 成绩录入与确认）';
