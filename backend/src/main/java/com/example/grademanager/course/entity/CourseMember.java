package com.example.grademanager.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_member")
public class CourseMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long studentId;

    /** 加入课程时的班级快照（BR05）：学生转班不影响既有分组。 */
    private Long classId;

    /** 快照班级名（冗余存储，班级改名/删除不影响历史分组展示）。 */
    private String className;

    /** 该生所在分组（课程+班级）成绩是否已确认发布。 */
    private Boolean groupConfirmed;

    private LocalDateTime createdAt;
}
