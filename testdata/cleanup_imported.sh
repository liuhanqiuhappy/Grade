#!/usr/bin/env bash
# 复位脚本：删除本次验证产生的班级（默认「软件工程2301班」）及其学生、登录账号
# 账号按下述三步精确删除：先快照 user_id -> 删学生 -> 按快照删账号，不做任何模糊匹配
# 用法：bash cleanup_imported.sh

set -u
DB="${DB:-grade_manager}"
MY="mysql -h127.0.0.1 -uroot -p$DB_PASSWORD --default-character-set=utf8mb4 --batch $DB"
CLASS_NAME="${CLASS_NAME:-软件工程2301班}"

echo "== 清理前 =="
$MY -e "select c.id, c.name, count(s.id) students from school_class c left join student s on s.class_id=c.id group by c.id,c.name;" 2>/dev/null

echo
echo "== 执行删除（单事务，按 classId 精确定位） =="
$MY -e "
SET @cid := (SELECT id FROM school_class WHERE name='$CLASS_NAME' LIMIT 1);
SELECT @cid AS target_class_id;
DROP TEMPORARY TABLE IF EXISTS tmp_uid;
CREATE TEMPORARY TABLE tmp_uid (user_id BIGINT PRIMARY KEY);
INSERT IGNORE INTO tmp_uid SELECT user_id FROM student WHERE class_id=@cid AND user_id IS NOT NULL;
SELECT count(*) AS snapshot_accounts FROM tmp_uid;
START TRANSACTION;
DELETE FROM score         WHERE student_id IN (SELECT id FROM student WHERE class_id=@cid); SELECT ROW_COUNT() AS del_score;
DELETE FROM course_member WHERE student_id IN (SELECT id FROM student WHERE class_id=@cid); SELECT ROW_COUNT() AS del_member;
DELETE FROM student       WHERE class_id=@cid;                                              SELECT ROW_COUNT() AS del_student;
DELETE FROM sys_user      WHERE id IN (SELECT user_id FROM tmp_uid);                        SELECT ROW_COUNT() AS del_account;
DELETE FROM school_class  WHERE id=@cid;                                                    SELECT ROW_COUNT() AS del_class;
COMMIT;
" 2>/dev/null

echo
echo "== 清理后 =="
$MY -e "
select 'school_class' t, count(*) n from school_class union all
select 'student', count(*) from student union all
select 'sys_user', count(*) from sys_user union all
select 'course_member', count(*) from course_member union all
select 'score', count(*) from score union all
select 'notice', count(*) from notice;
select id,name from school_class;
select id,username,role from sys_user;
" 2>/dev/null
