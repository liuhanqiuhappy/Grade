package com.example.grademanager.notice.dto;

import java.util.List;

/** 学生端下拉数据：一次请求同时给列表和未读数，避免红点与列表两个数据源对不上。 */
public record MyNoticePage(int unreadCount, List<MyNoticeView> items) {
}
