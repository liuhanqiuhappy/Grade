package com.example.grademanager.score.dto;

import java.util.List;

/**
 * 课程统计视图（需求 §6-6）：未录入按空值统计，不当作 0 分；不及格只统计已录入且低于 60 的成绩。
 */
public record CourseStatistics(long total, long unrecorded, long failed,
                               String status, List<GroupStat> groups) {

    /**
     * 按班级快照分组的统计摘要（confirmed = 该分组成绩是否已确认发布）。
     * classId 必须回传给前端：按分组确认时它就是确认粒度的唯一标识，
     * 缺失会退化成「确认全部分组」（需求 §6-4d 其他分组不受影响）。
     */
    public record GroupStat(Long classId, String className, long total, long unrecorded, long failed,
                            boolean confirmed) {
    }
}
