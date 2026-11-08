package com.example.grademanager.score.service;

import com.example.grademanager.score.dto.CourseStatistics;
import com.example.grademanager.score.dto.MyScoreView;
import com.example.grademanager.score.dto.SaveScoresResult;
import com.example.grademanager.score.dto.ScoreEntryRequest;
import com.example.grademanager.score.dto.ScoreImportResult;
import com.example.grademanager.score.dto.ScoreView;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ScoreService {

    /** 查看课程成绩表（含未录入行；仅本人课程）。 */
    List<ScoreView> listScores(Long courseId, Long teacherId);

    /** 批量保存成绩（仅 DRAFT 课程；已确认分组的成员行跳过不中断整批；乐观锁拦截并发覆盖）。 */
    SaveScoresResult saveScores(Long courseId, ScoreEntryRequest request, Long teacherId);

    /**
     * 确认发布成绩：分组内所有成员均已录入合法成绩才允许。
     * classId 为空 = 确认全部分组；否则只确认「课程 + 班级」分组，其他分组不受影响。
     * 全部分组确认后课程转 CONFIRMED。
     */
    void confirm(Long courseId, Long classId, Long teacherId);

    /** 课程统计：总人数 / 未录入 / 不及格，并按班级快照分组给出同样口径与确认状态。 */
    CourseStatistics statistics(Long courseId, Long teacherId);

    /** 学生自助查分：返回本人已加入的全部课程，未确认分组的统一标记为未公布。 */
    List<MyScoreView> myScores(Long userId);

    /**
     * Excel 批量导入成绩（仅未确认课程）：第 1 列学号、第 2 列分数，首行表头跳过。
     * 逐行校验，失败行记录原因但不中断整批；合法行按「课程 + 学号」新增或覆盖。
     * 已确认分组内的行跳过并记为失败（只读保护）。
     */
    ScoreImportResult importScores(Long courseId, MultipartFile file, Long teacherId);
}
