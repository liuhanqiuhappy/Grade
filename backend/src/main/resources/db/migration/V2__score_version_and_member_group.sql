-- V2：并发保护（score 乐观锁）+ 分组确认与班级快照（course_member）
-- 1) score 增加乐观锁版本列：多方同时保存时以 version 拦截静默覆盖（§4-1）
ALTER TABLE score
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER score;

-- 2) course_member 增加加入时的班级快照（BR05：转班不影响既有分组）与分组确认状态（§4-2/§4-8）
ALTER TABLE course_member
    ADD COLUMN class_id        BIGINT      NULL COMMENT '加入课程时的班级快照' AFTER student_id,
    ADD COLUMN class_name      VARCHAR(100) NULL COMMENT '快照班级名（冗余，防班级改名/删除影响历史分组）' AFTER class_id,
    ADD COLUMN group_confirmed TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '该生所在分组（课程+班级）成绩是否已确认发布' AFTER class_name;

-- 存量成员按学生当前班级回填快照（一次性迁移，之后不再跟随转班变化）
UPDATE course_member cm
    JOIN student s ON cm.student_id = s.id
    JOIN school_class c ON s.class_id = c.id
   SET cm.class_id = s.class_id,
       cm.class_name = c.name
 WHERE cm.class_id IS NULL;
