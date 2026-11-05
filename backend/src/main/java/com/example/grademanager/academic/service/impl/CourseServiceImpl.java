package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.dto.CourseCreateRequest;
import com.example.grademanager.academic.dto.CourseResponse;
import com.example.grademanager.academic.dto.CourseUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.entity.Course;
import com.example.grademanager.academic.mapper.CourseMapper;
import com.example.grademanager.academic.service.CourseService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.session.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 课程代码：1–32 位字母/数字/下划线/连字符 */
    private static final String COURSE_CODE_PATTERN = "^[A-Za-z0-9_-]{1,32}$";

    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public PageResult<CourseResponse> list(String keyword, Integer page, Integer size, SessionUser operator) {
        Long teacherId = requireTeacherId(operator);
        String kw = normalizeKeyword(keyword);
        int[] ps = normalizePage(page, size);

        LambdaQueryWrapper<Course> wrapper = Wrappers.lambdaQuery(Course.class)
                .eq(Course::getTeacherId, teacherId)
                .eq(Course::getStatus, 1);
        if (kw != null) {
            wrapper.and(w -> w.like(Course::getCourseCode, kw).or().like(Course::getCourseName, kw));
        }
        long total = courseMapper.selectCount(wrapper);
        wrapper.orderByDesc(Course::getCreatedAt)
                .last("LIMIT " + (long) (ps[0] - 1) * ps[1] + "," + ps[1]);
        List<Course> rows = courseMapper.selectList(wrapper);
        List<CourseResponse> list = rows.stream().map(this::toResponse).toList();
        return PageResult.of(list, total, ps[0], ps[1]);
    }

    @Override
    public CourseResponse get(Long courseId, SessionUser operator) {
        return toResponse(requireOwnedCourse(courseId, operator));
    }

    @Override
    public CourseResponse create(CourseCreateRequest request, SessionUser operator) {
        Long teacherId = requireTeacherId(operator);
        String code = normalizeCode(request.courseCode());
        String name = normalizeName(request.courseName());
        ensureCodeNotDuplicated(code, null);

        Course entity = new Course();
        entity.setCourseCode(code);
        entity.setCourseName(name);
        entity.setTeacherId(teacherId);
        entity.setStatus(1);
        try {
            courseMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.warn("新建课程唯一索引冲突：{}", e.getMessage());
            throw toCodeDuplicated(code, e);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        log.info("新建课程 courseCode={} teacherId={}", code, teacherId);
        return toResponse(entity);
    }

    @Override
    public CourseResponse update(Long courseId, CourseUpdateRequest request, SessionUser operator) {
        if (request.getCourseCode() == null && request.getCourseName() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "未提供任何可改字段");
        }
        Course entity = requireOwnedCourse(courseId, operator);

        String newCode = null;
        if (request.getCourseCode() != null) {
            newCode = normalizeCode(request.getCourseCode());
            ensureCodeNotDuplicated(newCode, courseId);
        }
        String newName = null;
        if (request.getCourseName() != null) {
            newName = normalizeName(request.getCourseName());
        }
        if (newCode != null) {
            entity.setCourseCode(newCode);
        }
        if (newName != null) {
            entity.setCourseName(newName);
        }
        try {
            courseMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            log.warn("修改课程唯一索引冲突：{}", e.getMessage());
            throw toCodeDuplicated(newCode, e);
        }
        return toResponse(entity);
    }

    @Override
    public CourseResponse archive(Long courseId, SessionUser operator) {
        Course entity = requireOwnedCourse(courseId, operator);
        entity.setStatus(0);
        courseMapper.updateById(entity);
        log.info("归档课程 courseId={} courseCode={}", courseId, entity.getCourseCode());
        return toResponse(entity);
    }

    private Course requireOwnedCourse(Long courseId, SessionUser operator) {
        Course entity = courseMapper.selectById(courseId);
        if (entity == null || entity.getStatus() == 0) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        Long teacherId = requireTeacherId(operator);
        if (!teacherId.equals(entity.getTeacherId())) {
            throw new BusinessException(ErrorCode.COURSE_FORBIDDEN);
        }
        return entity;
    }

    private static Long requireTeacherId(SessionUser operator) {
        if (operator == null || operator.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return operator.userId();
    }

    private void ensureCodeNotDuplicated(String code, Long excludeId) {
        Long count = courseMapper.selectCount(Wrappers.lambdaQuery(Course.class)
                .eq(Course::getCourseCode, code)
                .ne(excludeId != null, Course::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.COURSE_CODE_DUPLICATED, "课程代码 " + code + " 已被占用");
        }
    }

    private BusinessException toCodeDuplicated(String code, DuplicateKeyException e) {
        if (String.valueOf(e.getMessage()).contains("uk_course_code")) {
            return new BusinessException(ErrorCode.COURSE_CODE_DUPLICATED, "课程代码 " + code + " 已被占用");
        }
        throw e;
    }

    private CourseResponse toResponse(Course entity) {
        return new CourseResponse(entity.getId(), entity.getCourseCode(), entity.getCourseName(),
                entity.getStatus(),
                entity.getCreatedAt() == null ? null : TIME_FORMAT.format(entity.getCreatedAt()));
    }

    private static String normalizeCode(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "courseCode 不能为空");
        }
        String code = raw.trim();
        if (!code.matches(COURSE_CODE_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "课程代码须为 1–32 位字母/数字/下划线/连字符");
        }
        return code;
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "courseName 不能为空");
        }
        String name = raw.trim();
        if (name.isEmpty() || name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "courseName 去空格后须为 1–64 个字符");
        }
        return name;
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
