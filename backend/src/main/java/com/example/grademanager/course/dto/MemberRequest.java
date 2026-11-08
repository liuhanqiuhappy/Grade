package com.example.grademanager.course.dto;

import java.util.List;

/** 批量加入成员请求。 */
public record MemberRequest(@jakarta.validation.constraints.NotEmpty(message = "studentIds 不能为空")
                            List<Long> studentIds) {
}
