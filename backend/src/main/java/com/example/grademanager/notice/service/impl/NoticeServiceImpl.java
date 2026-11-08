package com.example.grademanager.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.ScoreRules;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.notice.dto.FailedStudentList;
import com.example.grademanager.notice.dto.FailedStudentView;
import com.example.grademanager.notice.dto.MyNoticePage;
import com.example.grademanager.notice.dto.MyNoticeView;
import com.example.grademanager.notice.dto.SendNoticeRequest;
import com.example.grademanager.notice.dto.SendNoticeResult;
import com.example.grademanager.notice.dto.TeacherNoticeView;
import com.example.grademanager.notice.entity.Notice;
import com.example.grademanager.notice.entity.NoticeRecipient;
import com.example.grademanager.notice.mapper.NoticeMapper;
import com.example.grademanager.notice.mapper.NoticeRecipientMapper;
import com.example.grademanager.notice.service.NoticeService;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl implements NoticeService {

    /** 学生端下拉一次最多返回的条数：通知量小，但别让前端无限拉。 */
    private static final int MY_NOTICE_LIMIT = 20;

    private final NoticeMapper noticeMapper;
    private final NoticeRecipientMapper recipientMapper;
    private final CourseMapper courseMapper;
    private final CourseMemberMapper memberMapper;
    private final StudentMapper studentMapper;
    private final ScoreMapper scoreMapper;
    private final CourseServiceImpl courseService;

    public NoticeServiceImpl(NoticeMapper noticeMapper, NoticeRecipientMapper recipientMapper,
                             CourseMapper courseMapper, CourseMemberMapper memberMapper,
                             StudentMapper studentMapper, ScoreMapper scoreMapper,
                             CourseServiceImpl courseService) {
        this.noticeMapper = noticeMapper;
        this.recipientMapper = recipientMapper;
        this.courseMapper = courseMapper;
        this.memberMapper = memberMapper;
        this.studentMapper = studentMapper;
        this.scoreMapper = scoreMapper;
        this.courseService = courseService;
    }

    @Override
    public FailedStudentList failedStudents(Long courseId, Long classId, Long teacherId) {
        // 归属校验与成绩模块同口径：他人课程一律按「不存在」拒绝（3001）
        courseService.requireOwnCourse(courseId, teacherId);

        List<CourseMember> members = members(courseId);
        List<CourseMember> scope = classId == null
                ? members
                : members.stream().filter(m -> classId.equals(m.getClassId())).toList();
        if (scope.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "该范围内没有课程成员");
        }

        // 只算已确认发布的分组：未确认的成绩对学生尚未公布，据此发补考通知等于提前泄露结果
        List<CourseMember> confirmed = scope.stream()
                .filter(m -> Boolean.TRUE.equals(m.getGroupConfirmed()))
                .toList();
        if (confirmed.isEmpty()) {
            throw new BusinessException(ErrorCode.SCORE_NOT_CONFIRMED,
                    "该范围成绩尚未确认发布，确认后才能生成挂科名单");
        }

        Map<Long, Score> scoreMap = scoresOfCourse(courseId);
        Set<Long> notified = notifiedStudentIds(courseId);
        Map<Long, Student> studentMap = studentsByIds(
                confirmed.stream().map(CourseMember::getStudentId).collect(Collectors.toSet()));

        List<FailedStudentView> items = confirmed.stream()
                .map(m -> {
                    Student s = studentMap.get(m.getStudentId());
                    if (s == null) {
                        return null;
                    }
                    Score sc = scoreMap.get(m.getStudentId());
                    if (sc == null || !ScoreRules.isFailed(sc.getScore())) {
                        return null;
                    }
                    return new FailedStudentView(s.getId(), s.getStudentNo(), s.getName(),
                            m.getClassName() == null ? "" : m.getClassName(),
                            sc.getScore(), s.getUserId() != null,
                            notified.contains(s.getId()));
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(FailedStudentView::className)
                        .thenComparing(FailedStudentView::score))
                .toList();

        int noAccount = (int) items.stream().filter(i -> !i.hasAccount()).count();
        return new FailedStudentList(items, items.size(), noAccount);
    }

    @Override
    @Transactional
    public SendNoticeResult send(Long courseId, SendNoticeRequest request, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);

        // 复用名单计算：未确认在这里就是 3007，避免绕过名单直接给未公布成绩的学生发通知
        FailedStudentList list = failedStudents(courseId, request.classId(), teacherId);
        if (list.items().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM,
                    "该范围没有挂科学生，无需发送补考通知");
        }

        Notice notice = new Notice();
        notice.setCourseId(courseId);
        notice.setClassId(request.classId());
        notice.setClassName(request.classId() == null ? null : list.items().get(0).className());
        notice.setType(Notice.TYPE_MAKEUP_EXAM);
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setExamTime(request.examTime());
        notice.setExamPlace(blankToNull(request.examPlace()));
        notice.setExamScope(blankToNull(request.examScope()));
        notice.setCreatedBy(teacherId);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setRevoked(false);
        noticeMapper.insert(notice);

        // 收件人在这里固化：之后名单、分数、成员再变都不影响这批已发通知
        // 学生档案一次批量取回，避免逐行 selectById（100 人就是 100 次往返）
        Map<Long, Student> studentMap = studentsByIds(list.items().stream()
                .map(FailedStudentView::studentId)
                .collect(Collectors.toSet()));
        int noAccount = 0;
        for (FailedStudentView item : list.items()) {
            Student student = studentMap.get(item.studentId());
            Long userId = student == null ? null : student.getUserId();
            if (userId == null) {
                noAccount++;
            }
            NoticeRecipient recipient = new NoticeRecipient();
            recipient.setNoticeId(notice.getId());
            recipient.setStudentId(item.studentId());
            recipient.setUserId(userId);
            recipient.setScoreSnapshot(item.score());
            recipient.setCreatedAt(LocalDateTime.now());
            recipientMapper.insert(recipient);
        }
        return new SendNoticeResult(notice.getId(), list.items().size(), noAccount);
    }

    @Override
    public List<TeacherNoticeView> listForTeacher(Long courseId, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getCourseId, courseId)
                .orderByDesc(Notice::getId));
        if (notices.isEmpty()) {
            return List.of();
        }
        Map<Long, List<NoticeRecipient>> byNotice = recipientMapper.selectList(
                        new LambdaQueryWrapper<NoticeRecipient>()
                                .in(NoticeRecipient::getNoticeId,
                                        notices.stream().map(Notice::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(NoticeRecipient::getNoticeId));

        return notices.stream()
                .map(n -> {
                    List<NoticeRecipient> recipients =
                            byNotice.getOrDefault(n.getId(), List.of());
                    int read = (int) recipients.stream().filter(r -> r.getReadAt() != null).count();
                    int noAccount = (int) recipients.stream().filter(r -> r.getUserId() == null).count();
                    return new TeacherNoticeView(n.getId(), n.getClassId(), n.getClassName(),
                            n.getTitle(), n.getContent(), n.getExamTime(), n.getExamPlace(),
                            n.getExamScope(), n.getCreatedAt(), Boolean.TRUE.equals(n.getRevoked()),
                            recipients.size(), read, noAccount);
                })
                .toList();
    }

    @Override
    public void revoke(Long courseId, Long noticeId, Long teacherId) {
        Notice notice = requireOwnNotice(courseId, noticeId, teacherId);
        if (Boolean.TRUE.equals(notice.getRevoked())) {
            return;
        }
        notice.setRevoked(true);
        noticeMapper.updateById(notice);
    }

    @Override
    public MyNoticePage myNotices(Long userId) {
        Student me = requireStudent(userId);
        List<NoticeRecipient> recipients = recipientMapper.selectList(
                new LambdaQueryWrapper<NoticeRecipient>()
                        .eq(NoticeRecipient::getStudentId, me.getId()));
        if (recipients.isEmpty()) {
            return new MyNoticePage(0, List.of());
        }

        // 撤回的通知既不出现在列表里，也不计入未读——否则红点永远消不掉
        Map<Long, Notice> noticeMap = noticesByIds(
                recipients.stream().map(NoticeRecipient::getNoticeId).collect(Collectors.toSet()));
        Map<Long, String> courseNames = courseNames(noticeMap.values());

        List<NoticeRecipient> visible = recipients.stream()
                .filter(r -> noticeMap.containsKey(r.getNoticeId()))
                .sorted(Comparator
                        .comparing((NoticeRecipient r) -> noticeMap.get(r.getNoticeId()).getCreatedAt())
                        .thenComparing(NoticeRecipient::getId)
                        .reversed())
                .toList();
        int unread = (int) visible.stream().filter(r -> r.getReadAt() == null).count();

        List<MyNoticeView> items = visible.stream()
                .limit(MY_NOTICE_LIMIT)
                .map(r -> {
                    Notice n = noticeMap.get(r.getNoticeId());
                    return new MyNoticeView(r.getId(), n.getId(),
                            courseNames.getOrDefault(n.getCourseId(), ""),
                            n.getTitle(), n.getContent(), n.getExamTime(), n.getExamPlace(),
                            n.getExamScope(), n.getCreatedAt(), r.getReadAt() != null);
                })
                .toList();
        return new MyNoticePage(unread, items);
    }

    @Override
    public void markRead(Long userId, Long recipientId) {
        Student me = requireStudent(userId);
        NoticeRecipient recipient = recipientMapper.selectById(recipientId);
        // 归属校验必须落在收件行上：只按 id 查会让任何学生把别人的通知标成已读
        if (recipient == null || !me.getId().equals(recipient.getStudentId())) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }
        if (recipient.getReadAt() != null) {
            return;
        }
        recipient.setReadAt(LocalDateTime.now());
        recipientMapper.updateById(recipient);
    }

    @Override
    public void markAllRead(Long userId) {
        Student me = requireStudent(userId);
        List<NoticeRecipient> unread = recipientMapper.selectList(
                new LambdaQueryWrapper<NoticeRecipient>()
                        .eq(NoticeRecipient::getStudentId, me.getId())
                        .isNull(NoticeRecipient::getReadAt));
        if (unread.isEmpty()) {
            return;
        }
        Set<Long> visibleNoticeIds = noticesByIds(
                unread.stream().map(NoticeRecipient::getNoticeId).collect(Collectors.toSet())).keySet();
        List<Long> ids = unread.stream()
                .filter(r -> visibleNoticeIds.contains(r.getNoticeId()))
                .map(NoticeRecipient::getId)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        recipientMapper.update(null, new LambdaUpdateWrapper<NoticeRecipient>()
                .in(NoticeRecipient::getId, ids)
                .set(NoticeRecipient::getReadAt, LocalDateTime.now()));
    }

    /** 取课程下未撤回通知已覆盖的学生集合，用于名单里的「已通知」标记。 */
    private Set<Long> notifiedStudentIds(Long courseId) {
        List<Long> noticeIds = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getCourseId, courseId)
                        .eq(Notice::getRevoked, false))
                .stream().map(Notice::getId).toList();
        if (noticeIds.isEmpty()) {
            return Set.of();
        }
        return recipientMapper.selectList(new LambdaQueryWrapper<NoticeRecipient>()
                        .in(NoticeRecipient::getNoticeId, noticeIds))
                .stream()
                .map(NoticeRecipient::getStudentId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** 按 id 批量取通知，顺带过滤掉已撤回的（撤回即对学生不可见）。 */
    private Map<Long, Notice> noticesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Notice> map = new LinkedHashMap<>();
        noticeMapper.selectBatchIds(ids).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getRevoked()))
                .forEach(n -> map.put(n.getId(), n));
        return map;
    }

    private Map<Long, String> courseNames(java.util.Collection<Notice> notices) {
        Set<Long> courseIds = notices.stream().map(Notice::getCourseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName, (a, b) -> a));
    }

    private Notice requireOwnNotice(Long courseId, Long noticeId, Long teacherId) {
        courseService.requireOwnCourse(courseId, teacherId);
        Notice notice = noticeMapper.selectById(noticeId);
        // 通知必须属于路径里的课程，否则就是拿自己课程的 URL 去动别人的通知
        if (notice == null || !courseId.equals(notice.getCourseId())) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    /** 学生端接口一律经 session 的 userId 反查档案，绝不接受前端传 studentId。 */
    private Student requireStudent(Long userId) {
        Student me = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (me == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定学生档案，无法查看通知");
        }
        return me;
    }

    private List<CourseMember> members(Long courseId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<CourseMember>().eq(CourseMember::getCourseId, courseId));
    }

    private Map<Long, Score> scoresOfCourse(Long courseId) {
        return scoreMapper.selectList(
                        new LambdaQueryWrapper<Score>().eq(Score::getCourseId, courseId)).stream()
                .collect(Collectors.toMap(Score::getStudentId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Student> studentsByIds(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentMapper.selectBatchIds(new ArrayList<>(studentIds)).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
