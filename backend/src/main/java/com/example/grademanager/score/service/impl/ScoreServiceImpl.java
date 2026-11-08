package com.example.grademanager.score.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.score.dto.CourseStatistics;
import com.example.grademanager.score.dto.MyScoreView;
import com.example.grademanager.score.dto.SaveScoresResult;
import com.example.grademanager.score.dto.ScoreEntry;
import com.example.grademanager.score.dto.ScoreEntryRequest;
import com.example.grademanager.score.dto.ScoreImportResult;
import com.example.grademanager.score.dto.ScoreView;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.score.service.ScoreService;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final ScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final CourseMemberMapper memberMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final CourseServiceImpl courseService;

    public ScoreServiceImpl(ScoreMapper scoreMapper, CourseMapper courseMapper,
                            CourseMemberMapper memberMapper, StudentMapper studentMapper,
                            UserMapper userMapper, CourseServiceImpl courseService) {
        this.scoreMapper = scoreMapper;
        this.courseMapper = courseMapper;
        this.memberMapper = memberMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.courseService = courseService;
    }

    @Override
    public List<ScoreView> listScores(Long courseId, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);
        List<CourseMember> members = members(courseId);
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> studentMap = studentsByIds(
                members.stream().map(CourseMember::getStudentId).collect(Collectors.toSet()));
        Map<Long, Score> scoreMap = scoresOfCourse(courseId);

        // 分组展示一律使用加入课程时的班级快照（BR05），不受学生后续转班影响
        return members.stream()
                .map(m -> {
                    Student s = studentMap.get(m.getStudentId());
                    if (s == null) {
                        return null;
                    }
                    Score sc = scoreMap.get(s.getId());
                    return new ScoreView(s.getId(), s.getStudentNo(), s.getName(),
                            m.getClassName() == null ? "" : m.getClassName(),
                            sc == null ? null : sc.getScore(),
                            sc == null ? null : sc.getVersion(),
                            Boolean.TRUE.equals(m.getGroupConfirmed()));
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public SaveScoresResult saveScores(Long courseId, ScoreEntryRequest request, Long teacherId) {
        Course course = courseService.requireOwnCourse(courseId, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED);
        }
        Map<Long, CourseMember> memberMap = members(courseId).stream()
                .collect(Collectors.toMap(CourseMember::getStudentId, Function.identity(), (a, b) -> a));
        Map<Long, Student> studentMap = studentsByIds(
                request.entries().stream().map(ScoreEntry::studentId).collect(Collectors.toSet()));

        int saved = 0;
        int skipped = 0;
        List<String> skippedStudents = new ArrayList<>();
        for (ScoreEntry entry : request.entries()) {
            CourseMember member = memberMap.get(entry.studentId());
            if (member == null) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        ErrorCode.MEMBER_NOT_FOUND.getMessage() + "（studentId=" + entry.studentId() + "）");
            }
            // 确认粒度 = 课程 + 班级：已确认分组的行跳过而不中断，保证其他分组照常保存（H1）
            if (Boolean.TRUE.equals(member.getGroupConfirmed())) {
                skipped++;
                skippedStudents.add(describeStudent(studentMap.get(entry.studentId()), entry.studentId()));
                continue;
            }
            validateRange(entry.score());
            Score existing = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                    .eq(Score::getCourseId, courseId)
                    .eq(Score::getStudentId, entry.studentId()));
            String who = describeStudent(studentMap.get(entry.studentId()), entry.studentId());
            if (existing == null) {
                // 客户端声明「这行已存在」（带了版本号）却查不到：期间被他人删除，按冲突处理
                if (entry.expectedVersion() != null) {
                    throw new BusinessException(ErrorCode.SCORE_CONFLICT,
                            who + " 的成绩已被他人修改，请刷新后重试");
                }
                Score score = new Score();
                score.setCourseId(courseId);
                score.setStudentId(entry.studentId());
                score.setScore(entry.score());
                score.setVersion(0);
                try {
                    scoreMapper.insert(score);
                } catch (DuplicateKeyException e) {
                    // 并发插入：另一窗口刚为本行建档，唯一键冲突等价于版本冲突
                    throw new BusinessException(ErrorCode.SCORE_CONFLICT,
                            who + " 的成绩已被他人修改，请刷新后重试");
                }
            } else {
                // §6-4e：先比客户端读取时的版本，挡住「陈旧页面覆盖」（毫秒级真并发由 @Version 兜底）
                if (entry.expectedVersion() != null
                        && !entry.expectedVersion().equals(existing.getVersion())) {
                    throw new BusinessException(ErrorCode.SCORE_CONFLICT,
                            who + " 的成绩已被他人修改，请刷新后重试");
                }
                existing.setScore(entry.score());
                if (scoreMapper.updateById(existing) == 0) {
                    throw new BusinessException(ErrorCode.SCORE_CONFLICT,
                            who + " 的成绩已被他人修改，请刷新后重试");
                }
            }
            saved++;
        }
        return new SaveScoresResult(saved, skipped, skippedStudents);
    }

    private String describeStudent(Student student, Long studentId) {
        if (student == null) {
            return "学生（ID=" + studentId + "）";
        }
        return student.getName() + "（" + student.getStudentNo() + "）";
    }

    @Override
    @Transactional
    public void confirm(Long courseId, Long classId, Long teacherId) {
        Course course = courseService.requireOwnCourse(courseId, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            return;
        }
        List<CourseMember> members = members(courseId);
        if (members.isEmpty()) {
            throw new BusinessException(ErrorCode.SCORE_INCOMPLETE, "该课程还没有成员，无法确认发布");
        }
        boolean byGroup = classId != null;
        List<CourseMember> targets = byGroup
                ? members.stream().filter(m -> classId.equals(m.getClassId())).toList()
                : members;
        if (targets.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "该班级在本课程中没有成员，无法按分组确认");
        }

        // 需求 §6-4c：目标分组内全员录入合法成绩才允许确认，并指名到人
        Set<Long> targetStudentIds = targets.stream().map(CourseMember::getStudentId).collect(Collectors.toSet());
        Map<Long, Score> scoreMap = scoresOfCourse(courseId);
        Map<Long, Student> studentMap = studentsByIds(targetStudentIds);
        List<String> missing = targets.stream()
                .filter(m -> {
                    Score s = scoreMap.get(m.getStudentId());
                    return s == null || s.getScore() == null;
                })
                .map(m -> {
                    Student s = studentMap.get(m.getStudentId());
                    String name = s == null ? "ID=" + m.getStudentId() : s.getName();
                    String no = s == null ? "" : s.getStudentNo();
                    return name + "（" + no + "）";
                })
                .toList();
        if (!missing.isEmpty()) {
            String scope = byGroup ? "分组内" : "";
            throw new BusinessException(ErrorCode.SCORE_INCOMPLETE,
                    "以下 " + missing.size() + " 名学生尚未录入成绩：" + scope + String.join("、", missing));
        }

        // 锁定目标分组；确认粒度 = 课程 + 班级，其他分组不受影响
        memberMapper.update(null, new LambdaUpdateWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .eq(byGroup, CourseMember::getClassId, classId)
                .set(CourseMember::getGroupConfirmed, true));

        // 全部分组都已确认 → 课程整体 CONFIRMED
        boolean allConfirmed = members.stream()
                .filter(m -> !targets.contains(m))
                .noneMatch(m -> !Boolean.TRUE.equals(m.getGroupConfirmed()));
        if (allConfirmed) {
            course.setStatus(Course.STATUS_CONFIRMED);
            courseMapper.updateById(course);
        }
    }

    @Override
    public CourseStatistics statistics(Long courseId, Long teacherId) {
        Course course = courseService.requireOwnCourse(courseId, teacherId);
        List<ScoreView> rows = listScores(courseId, teacherId);
        Map<Long, CourseMember> memberMap = members(courseId).stream()
                .collect(Collectors.toMap(CourseMember::getStudentId, Function.identity(), (a, b) -> a));

        long unrecorded = rows.stream().filter(r -> r.score() == null).count();
        long failed = rows.stream().filter(r -> isFailed(r.score())).count();

        // 按快照 classId 分组：classId 是「课程+班级」确认粒度的稳定标识，className 只用于展示
        Map<Long, List<ScoreView>> byClass = rows.stream().collect(Collectors.groupingBy(
                r -> classIdOf(memberMap.get(r.studentId())),
                LinkedHashMap::new, Collectors.toList()));

        List<CourseStatistics.GroupStat> groups = byClass.entrySet().stream()
                .map(e -> {
                    List<ScoreView> groupRows = e.getValue();
                    String className = groupRows.stream()
                            .map(ScoreView::className)
                            .filter(n -> n != null && !n.isBlank())
                            .findFirst().orElse("未分班");
                    return new CourseStatistics.GroupStat(
                            e.getKey(),
                            className,
                            groupRows.size(),
                            groupRows.stream().filter(r -> r.score() == null).count(),
                            groupRows.stream().filter(r -> isFailed(r.score())).count(),
                            groupRows.stream().allMatch(r -> {
                                CourseMember m = memberMap.get(r.studentId());
                                return m != null && Boolean.TRUE.equals(m.getGroupConfirmed());
                            }));
                })
                .toList();

        return new CourseStatistics(rows.size(), unrecorded, failed, course.getStatus(), groups);
    }

    /** 分组的稳定标识：取加入课程时的班级快照；历史脏数据（快照为空）返回 null，前端据此禁用该组确认。 */
    private Long classIdOf(CourseMember member) {
        return member == null ? null : member.getClassId();
    }

    /** 不及格只统计已录入的成绩；0 分计入不及格，空值不计入（需求 §6-6b）。 */
    private boolean isFailed(BigDecimal score) {
        return score != null && score.compareTo(new BigDecimal("60")) < 0;
    }

    @Override
    public List<MyScoreView> myScores(Long userId) {
        Student me = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (me == null) {
            return List.of();
        }
        List<CourseMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getStudentId, me.getId()));
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, CourseMember> membershipByCourse = memberships.stream()
                .collect(Collectors.toMap(CourseMember::getCourseId, Function.identity(), (a, b) -> a));
        List<Course> courses = courseMapper.selectBatchIds(membershipByCourse.keySet()).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<Long, String> teacherNames = userMapper.selectBatchIds(
                        courses.stream().map(Course::getTeacherId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName, (a, b) -> a));
        Map<Long, Score> scoreMap = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                        .eq(Score::getStudentId, me.getId())
                        .in(Score::getCourseId, courses.stream().map(Course::getId).toList())).stream()
                .collect(Collectors.toMap(Score::getCourseId, s -> s, (a, b) -> a));

        // 未确认的课程照常返回（对外显示「未公布」），但 score 一律为 null，不泄露任何数字
        return courses.stream()
                .map(c -> {
                    CourseMember membership = membershipByCourse.get(c.getId());
                    boolean published = membership != null && Boolean.TRUE.equals(membership.getGroupConfirmed());
                    BigDecimal score = published && scoreMap.containsKey(c.getId())
                            ? scoreMap.get(c.getId()).getScore() : null;
                    return new MyScoreView(c.getId(), c.getName(), c.getTerm(),
                            teacherNames.getOrDefault(c.getTeacherId(), ""), score, published);
                })
                .toList();
    }

    @Override
    @Transactional
    public ScoreImportResult importScores(Long courseId, MultipartFile file, Long teacherId) {
        Course course = courseService.requireOwnCourse(courseId, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件格式不支持：仅接受 .xlsx（Excel 2007 及以上），请另存为 .xlsx 后重试");
        }

        // 名单与现有成绩一次性载入：逐行校验时不再查库，导入 100 行只有 2 次查询
        Map<Long, CourseMember> memberMap = members(courseId).stream()
                .collect(Collectors.toMap(CourseMember::getStudentId, Function.identity(), (a, b) -> a));
        Map<String, Student> studentByNo = memberMap.isEmpty() ? Map.of()
                : studentMapper.selectBatchIds(memberMap.keySet()).stream()
                .collect(Collectors.toMap(Student::getStudentNo, Function.identity(), (a, b) -> a));
        Map<Long, Score> scoreMap = scoresOfCourse(courseId);

        List<PendingRow> pending = new ArrayList<>();
        List<ScoreImportResult.RowError> failures = new ArrayList<>();
        Set<String> seenNos = new HashSet<>();
        boolean hasDataRow = false;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String no = cellText(row, 0, formatter);
                String rawScore = cellText(row, 1, formatter);
                int excelRow = i + 1;
                if (no.isBlank() && rawScore.isBlank()) {
                    continue;
                }
                hasDataRow = true;
                if (no.isBlank()) {
                    failures.add(new ScoreImportResult.RowError(excelRow, "学号不能为空"));
                    continue;
                }
                if (rawScore.isBlank()) {
                    failures.add(new ScoreImportResult.RowError(excelRow, "分数不能为空（未录入的成绩请留空该行，不要导出到模板）"));
                    continue;
                }
                if (!seenNos.add(no)) {
                    failures.add(new ScoreImportResult.RowError(excelRow, "文件内学号重复: " + no));
                    continue;
                }
                Student student = studentByNo.get(no);
                if (student == null) {
                    failures.add(new ScoreImportResult.RowError(excelRow,
                            "学号不在本课程成员名单中: " + no));
                    continue;
                }
                BigDecimal value;
                try {
                    value = new BigDecimal(rawScore.trim());
                } catch (NumberFormatException e) {
                    failures.add(new ScoreImportResult.RowError(excelRow,
                            "分数必须是数字，当前值: " + rawScore));
                    continue;
                }
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                    failures.add(new ScoreImportResult.RowError(excelRow,
                            "分数必须在 0~100 之间，当前值: " + rawScore));
                    continue;
                }
                CourseMember member = memberMap.get(student.getId());
                if (member != null && Boolean.TRUE.equals(member.getGroupConfirmed())) {
                    // 分组已确认发布 = 只读，导入也不能改；记为失败行而非中断整批
                    failures.add(new ScoreImportResult.RowError(excelRow,
                            student.getName() + "（" + no + "）所在班级分组已确认发布，成绩只读"));
                    continue;
                }
                pending.add(new PendingRow(excelRow, student, value));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件解析失败：请确认文件为 .xlsx 且首行为表头（学号 / 分数）");
        }

        if (!hasDataRow) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件中没有可导入的数据行：请从第 2 行开始填写学号与分数");
        }

        int success = 0;
        for (PendingRow row : pending) {
            Score existing = scoreMap.get(row.student().getId());
            if (existing == null) {
                Score score = new Score();
                score.setCourseId(courseId);
                score.setStudentId(row.student().getId());
                score.setScore(row.score());
                score.setVersion(0);
                try {
                    scoreMapper.insert(score);
                } catch (DuplicateKeyException e) {
                    // 导入期间他人刚建了同一行：不静默覆盖，记为失败行
                    failures.add(new ScoreImportResult.RowError(row.excelRow(),
                            row.student().getName() + "（" + row.student().getStudentNo()
                                    + "）的成绩已被他人修改，请刷新后重试"));
                    continue;
                }
            } else {
                existing.setScore(row.score());
                if (existing.getVersion() == null) {
                    existing.setVersion(0);
                }
                // 导入是显式覆盖，不比 expectedVersion；@Version 只兜底毫秒级真并发
                if (scoreMapper.updateById(existing) == 0) {
                    failures.add(new ScoreImportResult.RowError(row.excelRow(),
                            row.student().getName() + "（" + row.student().getStudentNo()
                                    + "）的成绩已被他人修改，请刷新后重试"));
                    continue;
                }
            }
            success++;
        }
        // 失败明细按 Excel 行号排序，前端直接照着表格改
        failures.sort((a, b) -> Integer.compare(a.row(), b.row()));
        return new ScoreImportResult(success, failures);
    }

    /** 已通过校验、待写库的一行（保留 Excel 行号用于失败回显）。 */
    private record PendingRow(int excelRow, Student student, BigDecimal score) {
    }

    private String cellText(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private List<CourseMember> members(Long courseId) {        return memberMapper.selectList(
                new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getCourseId, courseId));
    }

    private Map<Long, Score> scoresOfCourse(Long courseId) {
        return scoreMapper.selectList(
                        new LambdaQueryWrapper<Score>().eq(Score::getCourseId, courseId)).stream()
                .collect(Collectors.toMap(Score::getStudentId, s -> s, (a, b) -> a));
    }

    private Map<Long, Student> studentsByIds(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
    }

    private void validateRange(BigDecimal score) {
        if (score == null) {
            return;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ErrorCode.SCORE_INVALID);
        }
    }
}
