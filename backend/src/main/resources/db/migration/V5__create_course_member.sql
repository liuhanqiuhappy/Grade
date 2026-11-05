-- 模块三 M3B：课程成员表
-- 一条记录表示某个学生是某门课的成员；同一学生在同一门课只允许一条
CREATE TABLE IF NOT EXISTS course_member
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，即接口里的 memberId',
    course_id  BIGINT UNSIGNED NOT NULL COMMENT 'course.id',
    student_id BIGINT UNSIGNED NOT NULL COMMENT 'student.id',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_student (course_id, student_id),
    KEY idx_cm_student_id (student_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='课程成员表（M3B 课程成员）';
