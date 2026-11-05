package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.dto.GradeBoardResponse;
import com.example.grademanager.academic.dto.GradeItemRequest;
import com.example.grademanager.academic.dto.GradeSaveRequest;
import com.example.grademanager.academic.dto.GradeView;
import com.example.grademanager.academic.entity.Course;
import com.example.grademanager.academic.entity.CourseMember;
import com.example.grademanager.academic.entity.Grade;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.CourseMapper;
import com.example.grademanager.academic.mapper.CourseMemberMapper;
import com.example.grademanager.academic.mapper.GradeMapper;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.GradeService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.session.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final CourseMapper courseMapper;
    private final CourseMemberMapper memberMapper;
    private final GradeMapper gradeMapper;
    private final StudentMapper studentMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;

    public GradeServiceImpl(CourseMapper courseMapper, CourseMemberMapper memberMapper, GradeMapper gradeMapper,
                            StudentMapper studentMapper, SchoolClassMapper classMapper, UserMapper userMapper) {
        this.courseMapper = courseMapper;
        this.memberMapper = memberMapper;
        this.gradeMapper = gradeMapper;
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
    }

    @Override
    public GradeBoardResponse board(Long courseId, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        List<CourseMember> members = listMembers(courseId);
        List<Grade> grades = listGrades(courseId);
        boolean confirmed = isConfirmed(members, grades);
        return new GradeBoardResponse(confirmed, buildViews(members, grades));
    }

    @Override
    @Transactional
    public int save(Long courseId, GradeSaveRequest request, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        List<CourseMember> members = listMembers(courseId);
        List<Grade> grades = listGrades(courseId);
        if (isConfirmed(members, grades)) {
            throw new BusinessException(ErrorCode.GRADE_ALREADY_CONFIRMED);
        }
        Map<Long, Long> memberStudentIds = members.stream()
                .collect(Collectors.toMap(CourseMember::getStudentId, CourseMember::getStudentId, (a, b) -> a));
        Map<Long, Grade> gradeMap = grades.stream()
                .collect(Collectors.toMap(Grade::getStudentId, g -> g, (a, b) -> a));

        int saved = 0;
        for (GradeItemRequest item : request.items()) {
            if (item.studentId() == null || !memberStudentIds.containsKey(item.studentId())) {
                throw new BusinessException(ErrorCode.GRADE_STUDENT_NOT_MEMBER,
                        "学生 " + item.studentId() + " 不是该课程成员");
            }
            BigDecimal score = normalizeScore(item.score());
            Grade existing = gradeMap.get(item.studentId());
            if (existing == null) {
                Grade grade = new Grade();
                grade.setCourseId(courseId);
                grade.setStudentId(item.studentId());
                grade.setScore(score);
                grade.setStatus(0);
                grade.setVersion(0);
                gradeMapper.insert(grade);
                saved++;
                continue;
            }
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                throw new BusinessException(ErrorCode.GRADE_ALREADY_CONFIRMED);
            }
            int currentVersion = existing.getVersion() == null ? 0 : existing.getVersion();
            Grade update = new Grade();
            update.setScore(score);
            update.setVersion(currentVersion + 1);
            update.setUpdatedAt(LocalDateTime.now());
            int affected = gradeMapper.update(update, Wrappers.lambdaUpdate(Grade.class)
                    .eq(Grade::getId, existing.getId())
                    .eq(Grade::getVersion, currentVersion));
            if (affected == 0) {
                throw new BusinessException(ErrorCode.GRADE_VERSION_CONFLICT);
            }
            existing.setScore(score);
            existing.setVersion(currentVersion + 1);
            saved++;
        }
        log.info("保存成绩草稿 courseId={} count={}", courseId, saved);
        return saved;
    }

    @Override
    @Transactional
    public GradeBoardResponse confirm(Long courseId, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        List<CourseMember> members = listMembers(courseId);
        if (members.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该课程还没有成员，无法确认成绩");
        }
        List<Grade> grades = listGrades(courseId);
        Map<Long, Grade> gradeMap = grades.stream()
                .collect(Collectors.toMap(Grade::getStudentId, g -> g, (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();

        for (CourseMember member : members) {
            Grade existing = gradeMap.get(member.getStudentId());
            if (existing == null) {
                Grade grade = new Grade();
                grade.setCourseId(courseId);
                grade.setStudentId(member.getStudentId());
                grade.setScore(null);
                grade.setStatus(1);
                grade.setVersion(0);
                grade.setConfirmedAt(now);
                gradeMapper.insert(grade);
                continue;
            }
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                continue;
            }
            int currentVersion = existing.getVersion() == null ? 0 : existing.getVersion();
            Grade update = new Grade();
            update.setStatus(1);
            update.setConfirmedAt(now);
            update.setVersion(currentVersion + 1);
            update.setUpdatedAt(now);
            int affected = gradeMapper.update(update, Wrappers.lambdaUpdate(Grade.class)
                    .eq(Grade::getId, existing.getId())
                    .eq(Grade::getVersion, currentVersion));
            if (affected == 0) {
                throw new BusinessException(ErrorCode.GRADE_VERSION_CONFLICT);
            }
        }
        log.info("确认发布成绩 courseId={} members={}", courseId, members.size());
        List<Grade> latest = listGrades(courseId);
        return new GradeBoardResponse(true, buildViews(members, latest));
    }

    private List<CourseMember> listMembers(Long courseId) {
        return memberMapper.selectList(Wrappers.lambdaQuery(CourseMember.class)
                .eq(CourseMember::getCourseId, courseId)
                .orderByAsc(CourseMember::getCreatedAt));
    }

    private List<Grade> listGrades(Long courseId) {
        return gradeMapper.selectList(Wrappers.lambdaQuery(Grade.class)
                .eq(Grade::getCourseId, courseId));
    }

    private static boolean isConfirmed(List<CourseMember> members, List<Grade> grades) {
        if (members.isEmpty() || grades.isEmpty() || grades.size() < members.size()) {
            return false;
        }
        return grades.stream().allMatch(g -> g.getStatus() != null && g.getStatus() == 1);
    }

    private List<GradeView> buildViews(List<CourseMember> members, List<Grade> grades) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, Grade> gradeMap = grades.stream()
                .collect(Collectors.toMap(Grade::getStudentId, g -> g, (a, b) -> a));

        List<Long> studentIds = members.stream().map(CourseMember::getStudentId).toList();
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, Student> studentMap = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        List<Long> userIds = students.stream().map(Student::getUserId).toList();
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<Long> classIds = students.stream().map(Student::getClassId).toList();
        Map<Long, SchoolClass> classMap = classIds.isEmpty() ? Map.of()
                : classMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(SchoolClass::getId, c -> c, (a, b) -> a));

        return members.stream().map(member -> {
            Student student = studentMap.get(member.getStudentId());
            Grade grade = gradeMap.get(member.getStudentId());
            String studentNo = student == null ? null : student.getStudentNo();
            String name = student == null ? "（学生已删除）"
                    : (userMap.get(student.getUserId()) == null ? "" : userMap.get(student.getUserId()).getDisplayName());
            String className = student == null ? null
                    : (classMap.get(student.getClassId()) == null ? null : classMap.get(student.getClassId()).getName());
            return new GradeView(
                    member.getStudentId(),
                    studentNo,
                    name,
                    className,
                    grade == null ? null : grade.getScore(),
                    grade == null ? 0 : grade.getStatus(),
                    grade == null ? null : formatTime(grade.getConfirmedAt()));
        }).toList();
    }

    private static BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分数必须在 0–100 之间");
        }
        return score;
    }

    private Course requireOwnedCourse(Long courseId, SessionUser operator) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (operator == null || operator.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (!operator.userId().equals(course.getTeacherId())) {
            throw new BusinessException(ErrorCode.COURSE_FORBIDDEN);
        }
        return course;
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? null : TIME_FORMAT.format(time);
    }
}
