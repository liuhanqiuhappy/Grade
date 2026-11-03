package com.example.grademanager.common;

/**
 * 契约 §2 错误码表。业务失败一律 HTTP 200。
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_CREDENTIALS(1001, "用户名或密码错误"),
    UNAUTHENTICATED(1002, "登录已过期，请重新登录"),
    FORBIDDEN(1003, "无权访问该功能"),
    /** 注册草案新增：用户名已被占用 */
    USERNAME_TAKEN(1004, "用户名已被注册"),
    PARAM_INVALID(4001, "参数校验失败"),
    INTERNAL_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
