package com.example.grademanager.score.controller;

import com.example.grademanager.common.ApiResponse;
import com.example.grademanager.identity.session.SessionUser;
import com.example.grademanager.score.dto.ConfirmRequest;
import com.example.grademanager.score.dto.CourseStatistics;
import com.example.grademanager.score.dto.MyScoreView;
import com.example.grademanager.score.dto.SaveScoresResult;
import com.example.grademanager.score.dto.ScoreEntryRequest;
import com.example.grademanager.score.dto.ScoreImportResult;
import com.example.grademanager.score.dto.ScoreView;
import com.example.grademanager.score.service.ScoreService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 成绩录入与确认（模块四）：教师录入/确认；学生自助查已发布成绩。 */
@RestController
@RequestMapping("/api")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/courses/{courseId}/scores")
    public ApiResponse<List<ScoreView>> list(@PathVariable Long courseId, HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(scoreService.listScores(courseId, teacherId));
    }

    @PutMapping("/courses/{courseId}/scores")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long courseId,
                                                 @Valid @RequestBody ScoreEntryRequest request,
                                                 HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        SaveScoresResult result = scoreService.saveScores(courseId, request, teacherId);
        return ApiResponse.ok(Map.of(
                "saved", result.saved(),
                "skipped", result.skipped(),
                "skippedStudents", result.skippedStudents()));
    }

    /** 批量导入成绩：第 1 列学号、第 2 列分数，首行表头；失败行只汇报不中断，合法行正常入库。 */
    @PostMapping("/courses/{courseId}/scores/import")
    public ApiResponse<ScoreImportResult> importScores(@PathVariable Long courseId,
                                                       @RequestParam("file") MultipartFile file,
                                                       HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(scoreService.importScores(courseId, file, teacherId));
    }

    /** 确认发布：body 可省略（确认全部分组）；携带 classId 时按「课程 + 班级」分组确认。 */
    @PostMapping("/courses/{courseId}/scores/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long courseId,
                                     @Valid @RequestBody(required = false) ConfirmRequest request,
                                     HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        scoreService.confirm(courseId, request == null ? null : request.classId(), teacherId);
        return ApiResponse.ok();
    }

    /** 课程统计（模块六）：总人数 / 未录入 / 不及格，按班级快照分组（含确认状态）。 */
    @GetMapping("/courses/{courseId}/statistics")
    public ApiResponse<CourseStatistics> statistics(@PathVariable Long courseId, HttpSession session) {
        Long teacherId = SessionUser.requireTeacher(session).getUserId();
        return ApiResponse.ok(scoreService.statistics(courseId, teacherId));
    }

    @GetMapping("/my/scores")
    public ApiResponse<List<MyScoreView>> myScores(HttpSession session) {
        // 学生自助接口：教师访问一律 1003，不能用「查不到档案返回空列表」兜底
        SessionUser current = SessionUser.requireStudent(session);
        return ApiResponse.ok(scoreService.myScores(current.getUserId()));
    }
}
