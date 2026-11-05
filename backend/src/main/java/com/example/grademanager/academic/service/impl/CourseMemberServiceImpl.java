package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.dto.ClassMemberAddRequest;
import com.example.grademanager.academic.dto.ClassMemberAddResponse;
import com.example.grademanager.academic.dto.CourseMemberAddRequest;
import com.example.grademanager.academic.dto.CourseMemberView;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.entity.Course;
import com.example.grademanager.academic.entity.CourseMember;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.CourseMapper;
import com.example.grademanager.academic.mapper.CourseMemberMapper;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.CourseMemberService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.session.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseMemberServiceImpl implements CourseMemberService {

    private static final Logger log = LoggerFactory.getLogger(CourseMemberServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CourseMapper courseMapper;
    private final CourseMemberMapper memberMapper;
    private final StudentMapper studentMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;

    public CourseMemberServiceImpl(CourseMapper courseMapper, CourseMemberMapper memberMapper,
                                   StudentMapper studentMapper, SchoolClassMapper classMapper,
                                   UserMapper userMapper) {
        this.courseMapper = courseMapper;
        this.memberMapper = memberMapper;
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<CourseMemberView> list(Long courseId, String keyword, Integer page, Integer size,
                                             SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        String kw = normalizeKeyword(keyword);
        int[] ps = normalizePage(page, size);

        LambdaQueryWrapper<CourseMember> wrapper = Wrappers.lambdaQuery(CourseMember.class)
                .eq(CourseMember::getCourseId, courseId);
        if (kw != null) {
            List<Long> studentIds = studentMapper.selectList(Wrappers.lambdaQuery(Student.class)
                            .like(Student::getStudentNo, kw))
                    .stream().map(Student::getId).toList();
            if (studentIds.isEmpty()) {
                return PageResult.of(List.of(), 0, ps[0], ps[1]);
            }
            wrapper.in(CourseMember::getStudentId, studentIds);
        }
        long total = memberMapper.selectCount(wrapper);
        wrapper.orderByDesc(CourseMember::getCreatedAt)
                .last("LIMIT " + (long) (ps[0] - 1) * ps[1] + "," + ps[1]);
        List<CourseMember> rows = memberMapper.selectList(wrapper);
        return PageResult.of(toViews(rows), total, ps[0], ps[1]);
    }

    @Override
    public CourseMemberView add(Long courseId, CourseMemberAddRequest request, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        String studentNo = request.studentNo() == null ? "" : request.studentNo().trim();
        Student student = studentMapper.selectOne(Wrappers.lambdaQuery(Student.class)
                .eq(Student::getStudentNo, studentNo));
        if (student == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "学号 " + studentNo + " 不存在");
        }
        ensureNotMember(courseId, student.getId());

        CourseMember member = new CourseMember();
        member.setCourseId(courseId);
        member.setStudentId(student.getId());
        member.setCreatedAt(LocalDateTime.now());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            log.warn("课程成员唯一索引冲突：{}", e.getMessage());
            throw new BusinessException(ErrorCode.COURSE_MEMBER_DUPLICATED);
        }
        log.info("加入课程成员 courseId={} studentNo={}", courseId, studentNo);
        return toViews(List.of(member)).get(0);
    }

    @Override
    @Transactional
    public ClassMemberAddResponse addByClass(Long courseId, ClassMemberAddRequest request, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        SchoolClass clazz = classMapper.selectById(request.classId());
        if (clazz == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        if (clazz.getStatus() != null && clazz.getStatus() == 0) {
            throw new BusinessException(ErrorCode.TARGET_CLASS_ARCHIVED);
        }
        List<Student> students = studentMapper.selectList(Wrappers.lambdaQuery(Student.class)
                .eq(Student::getClassId, request.classId())
                .eq(Student::getStatus, 1));

        int added = 0;
        int skipped = 0;
        for (Student student : students) {
            Long exists = memberMapper.selectCount(Wrappers.lambdaQuery(CourseMember.class)
                    .eq(CourseMember::getCourseId, courseId)
                    .eq(CourseMember::getStudentId, student.getId()));
            if (exists != null && exists > 0) {
                skipped++;
                continue;
            }
            CourseMember member = new CourseMember();
            member.setCourseId(courseId);
            member.setStudentId(student.getId());
            member.setCreatedAt(LocalDateTime.now());
            try {
                memberMapper.insert(member);
                added++;
            } catch (DuplicateKeyException e) {
                skipped++;
            }
        }
        log.info("按班加入课程成员 courseId={} classId={} total={} added={} skipped={}",
                courseId, request.classId(), students.size(), added, skipped);
        return new ClassMemberAddResponse(courseId, request.classId(), students.size(), added, skipped);
    }

    @Override
    public void remove(Long courseId, Long studentId, SessionUser operator) {
        requireOwnedCourse(courseId, operator);
        int affected = memberMapper.delete(Wrappers.lambdaQuery(CourseMember.class)
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getStudentId, studentId));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COURSE_MEMBER_NOT_FOUND);
        }
        log.info("移出课程成员 courseId={} studentId={}", courseId, studentId);
    }

    private void ensureNotMember(Long courseId, Long studentId) {
        Long count = memberMapper.selectCount(Wrappers.lambdaQuery(CourseMember.class)
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getStudentId, studentId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.COURSE_MEMBER_DUPLICATED);
        }
    }

    private List<CourseMemberView> toViews(List<CourseMember> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = rows.stream().map(CourseMember::getStudentId).toList();
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

        return rows.stream().map(member -> {
            Student student = studentMap.get(member.getStudentId());
            if (student == null) {
                return new CourseMemberView(member.getId(), member.getStudentId(), null,
                        "（学生已删除）", null, null, 0, formatTime(member.getCreatedAt()));
            }
            User user = userMap.get(student.getUserId());
            SchoolClass clazz = classMap.get(student.getClassId());
            return new CourseMemberView(
                    member.getId(),
                    student.getId(),
                    student.getStudentNo(),
                    user == null ? "" : user.getDisplayName(),
                    student.getClassId(),
                    clazz == null ? null : clazz.getName(),
                    student.getStatus(),
                    formatTime(member.getCreatedAt()));
        }).toList();
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

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String kw = keyword.trim();
        if (kw.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "keyword 最长 64 个字符");
        }
        return kw;
    }

    private static int[] normalizePage(Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? 10 : size;
        if (p < 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "page 必须 ≥1");
        }
        if (s < 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "size 必须在 1–100 之间");
        }
        if (s > 100) {
            s = 100;
        }
        return new int[]{p, s};
    }
}
