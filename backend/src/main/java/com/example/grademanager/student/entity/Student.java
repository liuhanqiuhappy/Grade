package com.example.grademanager.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String name;

    private String gender;

    private Long classId;

    /** 绑定的登录账号（学生自助查分），可为空。 */
    private Long userId;

    private LocalDateTime createdAt;
}
