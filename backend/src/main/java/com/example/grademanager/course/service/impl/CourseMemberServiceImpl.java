package com.example.grademanager.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.course.dto.MemberAddResult;
import com.example.grademanager.course.dto.MemberView;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.CourseMemberService;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseMemberServiceImpl implements CourseMemberService {

    private final CourseServiceImpl courseService;
    private final CourseMemberMapper memberMapper;
    private final StudentMapper studentMapper;
    private final SchoolClassMapper classMapper;
    private final ScoreMapper scoreMapper;

    public CourseMemberServiceImpl(CourseServiceImpl courseService, CourseMemberMapper memberMapper,
                                   StudentMapper studentMapper, SchoolClassMapper classMapper,
                                   ScoreMapper scoreMapper) {
        this.courseService = courseService;
        this.memberMapper = memberMapper;
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.scoreMapper = scoreMapper;
    }

    @Override
    public List<MemberView> listMembers(Long courseId, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);
        List<CourseMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getCourseId, courseId));
        Set<Long> studentIds = members.stream().map(CourseMember::getStudentId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
        // 展示加入课程时的班级快照（BR05），不随学生转班漂移
        return members.stream()
                .map(m -> {
                    Student s = studentMap.get(m.getStudentId());
                    if (s == null) {
                        return null;
                    }
                    return new MemberView(s.getId(), s.getStudentNo(), s.getName(),
                            m.getClassName() == null ? "" : m.getClassName(),
                            m.getClassId(),
                            Boolean.TRUE.equals(m.getGroupConfirmed()));
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public MemberAddResult addMembers(Long courseId, List<Long> studentIds, Long teacherId) {
        Course course = courseService.requireOwnCourse(courseId, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED, "课程已确认发布，不可再加入成员");
        }
        List<CourseMember> existing = memberMapper.selectList(
                new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getCourseId, courseId));
        Set<Long> existingIds = existing.stream().map(CourseMember::getStudentId).collect(Collectors.toSet());
        // 已确认发布的分组不再接受新成员（M3）：否则新行 group_confirmed=false 会把整组状态从「已公布」
        // 回退成「草稿」，同组其他学生显示已公布、本人显示未公布，确认结果的稳定性被破坏。
        Set<Long> confirmedClassIds = existing.stream()
                .filter(m -> Boolean.TRUE.equals(m.getGroupConfirmed()) && m.getClassId() != null)
                .map(CourseMember::getClassId)
                .collect(Collectors.toSet());

        int added = 0;
        int skipped = 0;
        int skippedConfirmed = 0;
        for (Long studentId : studentIds.stream().distinct().toList()) {
            // §6-3b：已在课程中的成员只跳过并计数，不再让整批加入失败
            if (existingIds.contains(studentId)) {
                skipped++;
                continue;
            }
            Student student = studentMapper.selectById(studentId);
            if (student == null) {
                throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND,
                        ErrorCode.STUDENT_NOT_FOUND.getMessage() + "（studentId=" + studentId + "）");
            }
            if (student.getClassId() != null && confirmedClassIds.contains(student.getClassId())) {
                skipped++;
                skippedConfirmed++;
                continue;
            }
            CourseMember member = new CourseMember();
            member.setCourseId(courseId);
            member.setStudentId(studentId);
            // BR05：加入时快照学生当前班级（含班级名），后续转班不影响本课程分组
            member.setClassId(student.getClassId());
            SchoolClass clazz = student.getClassId() == null ? null : classMapper.selectById(student.getClassId());
            member.setClassName(clazz == null ? null : clazz.getName());
            member.setGroupConfirmed(false);
            memberMapper.insert(member);
            existingIds.add(studentId);
            added++;
        }
        return new MemberAddResult(added, skipped, skippedConfirmed);
    }

    @Override
    @Transactional
    public void removeMember(Long courseId, Long studentId, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);
        CourseMember member = memberMapper.selectOne(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getCourseId, courseId)
                .eq(CourseMember::getStudentId, studentId));
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(member.getGroupConfirmed())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED, "该生所在分组成绩已确认发布，成员不可移出");
        }
        memberMapper.deleteById(member.getId());
        // 一并清理其暂存成绩，避免残留孤儿数据
        scoreMapper.delete(new LambdaQueryWrapper<Score>()
                .eq(Score::getCourseId, courseId)
                .eq(Score::getStudentId, studentId));
    }
}
