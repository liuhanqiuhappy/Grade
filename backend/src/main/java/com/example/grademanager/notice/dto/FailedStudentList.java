package com.example.grademanager.notice.dto;

import java.util.List;

/** 挂科名单汇总：total 为该范围内的挂科人数，noAccount 为其中无登录账号（发不达）的人数。 */
public record FailedStudentList(List<FailedStudentView> items, int total, int noAccount) {
}
