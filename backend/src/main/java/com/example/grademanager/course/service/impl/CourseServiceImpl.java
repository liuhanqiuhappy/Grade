package com.example.grademanager.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.course.dto.CourseRequest;
import com.example.grademanager.course.dto.CourseView;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.CourseService;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final ScoreMapper scoreMapper;

    public CourseServiceImpl(CourseMapper courseMapper, CourseMemberMapper memberMapper,
                             UserMapper userMapper, ScoreMapper scoreMapper) {
        this.courseMapper = courseMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.scoreMapper = scoreMapper;
    }

    @Override
    public List<CourseView> list(String keyword, Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        // 需求 §6-3a：只列出本教师创建的课程
        wrapper.eq(Course::getTeacherId, teacherId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Course::getName, kw).or().like(Course::getTerm, kw));
        }
        wrapper.orderByDesc(Course::getId);
        List<Course> courses = courseMapper.selectList(wrapper);
        return toViews(courses);
    }

    @Override
    public CourseView create(CourseRequest request, Long teacherId) {
        requireNameTermUnique(null, request);
        Course course = new Course();
        course.setName(request.name().trim());
        course.setTerm(request.term().trim());
        course.setStatus(Course.STATUS_DRAFT);
        course.setTeacherId(teacherId);
        courseMapper.insert(course);
        return toView(course);
    }

    @Override
    public CourseView update(Long id, CourseRequest request, Long teacherId) {
        Course course = requireOwnCourse(id, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED);
        }
        requireNameTermUnique(id, request);
        course.setName(request.name().trim());
        course.setTerm(request.term().trim());
        courseMapper.updateById(course);
        return toView(course);
    }

    @Override
    @Transactional
    public void delete(Long id, Long teacherId) {
        Course course = requireOwnCourse(id, teacherId);
        if (Course.STATUS_CONFIRMED.equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.COURSE_CONFIRMED);
        }
        // 删除顺序必须自底向上：score / course_member 都有指向 course 的外键且未设级联，
        // 先删子表再删主表，否则残留成绩行会让 deleteById 撞外键、以 5000 收场。
        scoreMapper.delete(new LambdaQueryWrapper<Score>().eq(Score::getCourseId, id));
        memberMapper.delete(new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getCourseId, id));
        courseMapper.deleteById(id);
    }

    /** 内部按 ID 取课程（不做归属校验，供成绩等子域复用）。 */
    public Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    /** 归属校验：不是本人的课程一律按"不存在"处理，避免泄露他人课程 ID 是否有效。供成绩/成员子域复用。 */
    public Course requireOwnCourse(Long id, Long teacherId) {
        Course course = requireCourse(id);
        if (teacherId != null && !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void requireNameTermUnique(Long excludeId, CourseRequest request) {
        Course existing = courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                .eq(Course::getName, request.name().trim())
                .eq(Course::getTerm, request.term().trim()));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.COURSE_NAME_DUP);
        }
    }

    private List<CourseView> toViews(List<Course> courses) {
        Set<Long> teacherIds = courses.stream().map(Course::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNames = teacherIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getDisplayName, (a, b) -> a));
        return courses.stream()
                .map(c -> new CourseView(c.getId(), c.getName(), c.getTerm(), c.getTeacherId(),
                        teacherNames.getOrDefault(c.getTeacherId(), ""), c.getStatus(),
                        memberMapper.selectCount(new LambdaQueryWrapper<CourseMember>()
                                .eq(CourseMember::getCourseId, c.getId()))))
                .toList();
    }

    private CourseView toView(Course course) {
        return toViews(List.of(course)).get(0);
    }
}
