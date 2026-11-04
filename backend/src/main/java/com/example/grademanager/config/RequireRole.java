package com.example.grademanager.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需角色（契约《模块二-班级与学生》§7.4）。
 * 标注在 Controller 类或方法上，由 {@link RoleAuthInterceptor} 在登录校验之后执行；
 * 身份只取 Session 中的 {@code SessionUser.role}，任何请求参数里的角色自述不采信。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，如 "TEACHER" */
    String value();
}
