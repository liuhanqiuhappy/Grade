package com.example.grademanager.common;

/**
 * 契约 §2 错误码表。业务失败一律 HTTP 200。
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_CREDENTIALS(1001, "用户名或密码错误"),
    UNAUTHENTICATED(1002, "登录已过期，请重新登录"),
    FORBIDDEN(1003, "无权访问该功能"),
    PARAM_INVALID(4001, "参数校验失败"),
    INTERNAL_ERROR(5000, "系统繁忙，请稍后重试"),

    // ===== 模块二：班级与学生（2xxx 号段归模块二独占，只在末尾追加，禁止复用空号）=====
    CLASS_NAME_DUPLICATED(2001, "班级名称已存在"),
    CLASS_NOT_FOUND(2002, "班级不存在或已归档"),
    STUDENT_NO_DUPLICATED(2003, "学号已被占用"),
    STUDENT_NOT_FOUND(2004, "学生不存在"),
    CLASS_HAS_DRAFT_GRADES(2005, "班级存在未发布成绩，不可归档"),
    TARGET_CLASS_ARCHIVED(2006, "目标班级已归档，不能接收学生"),
    USERNAME_DUPLICATED(2007, "登录名已被占用"),

    // ===== 模块三：课程与课程成员（3xxx 号段，M3A/M3B 使用）=====
    COURSE_CODE_DUPLICATED(3001, "课程代码已存在"),
    COURSE_NOT_FOUND(3002, "课程不存在或已归档"),
    COURSE_FORBIDDEN(3003, "无权操作他人的课程"),
    COURSE_MEMBER_NOT_FOUND(3004, "课程成员不存在"),
    COURSE_MEMBER_DUPLICATED(3005, "该学生已在课程成员中"),

    // ===== 模块四：成绩录入与确认（3xxx 号段继续）=====
    GRADE_STUDENT_NOT_MEMBER(3006, "该学生不是此课程成员"),
    GRADE_ALREADY_CONFIRMED(3007, "课程成绩已确认发布，不可修改"),
    GRADE_VERSION_CONFLICT(3008, "成绩已被他人修改，请刷新后重试");

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
