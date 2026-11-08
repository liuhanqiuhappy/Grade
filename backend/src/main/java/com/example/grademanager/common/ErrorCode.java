package com.example.grademanager.common;

import lombok.Getter;

/**
 * 业务错误码分段：
 * 1001-1003 身份权限；2001-2006 班级与学生；3001-3006 课程/成员/成绩；4001 参数错误；5000 系统错误。
 */
@Getter
public enum ErrorCode {
    BAD_CREDENTIALS(1001, "用户名或密码错误"),
    NOT_LOGIN(1002, "未登录或会话已过期"),
    FORBIDDEN(1003, "无权执行该操作"),

    CLASS_NOT_FOUND(2001, "班级不存在"),
    CLASS_NAME_DUP(2002, "班级名称已存在"),
    CLASS_NOT_EMPTY(2003, "班级下仍有学生，无法删除"),
    STUDENT_NOT_FOUND(2004, "学生不存在"),
    STUDENT_NO_DUP(2005, "学号已存在"),
    IMPORT_PARSE_FAIL(2006, "Excel 文件解析失败，请使用模板格式"),
    STUDENT_IN_USE(2007, "学生已加入课程或存在成绩，无法删除"),

    COURSE_NOT_FOUND(3001, "课程不存在"),
    COURSE_NAME_DUP(3002, "同名同学期课程已存在"),
    COURSE_CONFIRMED(3003, "成绩已确认发布，课程数据不可再修改"),
    MEMBER_EXISTS(3004, "该学生已在课程成员中"),
    MEMBER_NOT_FOUND(3005, "课程成员不存在"),
    SCORE_INVALID(3006, "成绩必须在 0~100 之间"),
    SCORE_NOT_CONFIRMED(3007, "成绩尚未确认发布"),
    SCORE_INCOMPLETE(3008, "存在未录入成绩的学生，无法确认发布"),
    STATISTICS_UNAVAILABLE(3009, "统计暂不可用"),
    SCORE_CONFLICT(3010, "成绩已被他人修改，请刷新后重试"),

    INVALID_PARAM(4001, "参数缺失或非法"),
    SYS_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
