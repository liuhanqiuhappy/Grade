package com.example.grademanager.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String term;

    private Long teacherId;

    private String status;

    private LocalDateTime createdAt;
}
