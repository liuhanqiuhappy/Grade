package com.example.grademanager.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    /** 1 = 下次登录后必须先修改口令（新建/导入/被重置的账号）。 */
    private Boolean mustChangePassword;

    /** 口令最近一次变更（含重置）时间。 */
    private LocalDateTime passwordUpdatedAt;

    /** 最近一次被教师重置口令的操作人（sys_user.id），自助改密为 null。 */
    private Long passwordResetBy;

    private String displayName;

    private String role;

    private LocalDateTime createdAt;
}
