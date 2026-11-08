package com.example.grademanager.notice.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.notice.dto.FailedStudentList;
import com.example.grademanager.notice.dto.MyNoticePage;
import com.example.grademanager.notice.dto.SendNoticeRequest;
import com.example.grademanager.notice.dto.SendNoticeResult;
import com.example.grademanager.notice.entity.Notice;
import com.example.grademanager.notice.entity.NoticeRecipient;
import com.example.grademanager.notice.mapper.NoticeMapper;
import com.example.grademanager.notice.mapper.NoticeRecipientMapper;
import com.example.grademanager.notice.service.impl.NoticeServiceImpl;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补考通知单测：挂科名单口径（只算已确认分组）/ 越权 / 发送收件人固化 / 已读归属与幂等。
 * 重点守住两条业务底线：未公布成绩不能发通知；学生只能读自己的收件行。
 */
class NoticeServiceImplTest {

    private NoticeMapper noticeMapper;
    private NoticeRecipientMapper recipientMapper;
    private CourseMapper courseMapper;
    private CourseMemberMapper memberMapper;
    private StudentMapper studentMapper;
    private ScoreMapper scoreMapper;
    private NoticeServiceImpl service;

    private Course course;

    @BeforeAll
    static void initEntityMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Notice.class);
        TableInfoHelper.initTableInfo(assistant, NoticeRecipient.class);
        TableInfoHelper.initTableInfo(assistant, Course.class);
        TableInfoHelper.initTableInfo(assistant, CourseMember.class);
        TableInfoHelper.initTableInfo(assistant, Student.class);
        TableInfoHelper.initTableInfo(assistant, Score.class);
    }

    @BeforeEach
    void setUp() {
        noticeMapper = Mockito.mock(NoticeMapper.class);
        recipientMapper = Mockito.mock(NoticeRecipientMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        memberMapper = Mockito.mock(CourseMemberMapper.class);
        studentMapper = Mockito.mock(StudentMapper.class);
        scoreMapper = Mockito.mock(ScoreMapper.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);

        CourseServiceImpl courseService = new CourseServiceImpl(courseMapper, memberMapper, userMapper,
                scoreMapper, noticeMapper, recipientMapper);
        service = new NoticeServiceImpl(noticeMapper, recipientMapper, courseMapper, memberMapper,
                studentMapper, scoreMapper, courseService);

        course = new Course();
        course.setId(10L);
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(1L);
        course.setStatus(Course.STATUS_DRAFT);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(noticeMapper.selectList(any())).thenReturn(List.of());
        when(recipientMapper.selectList(any())).thenReturn(List.of());
        when(studentMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
        when(courseMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
    }

    private CourseMember member(long studentId, long classId, String className, boolean confirmed) {
        CourseMember m = new CourseMember();
        m.setCourseId(10L);
        m.setStudentId(studentId);
        m.setClassId(classId);
        m.setClassName(className);
        m.setGroupConfirmed(confirmed);
        return m;
    }

    private Student student(long id, String no, String name, Long userId) {
        Student s = new Student();
        s.setId(id);
        s.setStudentNo(no);
        s.setName(name);
        s.setClassId(100L);
        s.setUserId(userId);
        return s;
    }

    private Score score(long studentId, String value) {
        Score s = new Score();
        s.setCourseId(10L);
        s.setStudentId(studentId);
        s.setScore(new BigDecimal(value));
        s.setVersion(0);
        return s;
    }

    @Test
    void failedStudentsShouldOnlyIncludeConfirmedGroups() {
        // 1 号已确认 58 分（挂科）、2 号已确认 75 分（不挂）、3 号未确认 40 分（不出现）
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 100L, "计算机2301班", true),
                member(2L, 100L, "计算机2301班", true),
                member(3L, 101L, "计算机2302班", false)));
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                score(1L, "58"), score(2L, "75"), score(3L, "40")));
        when(studentMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                student(1L, "2023010101", "李同学", 9L),
                student(2L, "2023010102", "王同学", 10L),
                student(3L, "2023010103", "赵同学", 11L)));

        FailedStudentList list = service.failedStudents(10L, null, 1L);

        assertThat(list.total()).isEqualTo(1);
        assertThat(list.items().get(0).studentNo()).isEqualTo("2023010101");
        assertThat(list.items().get(0).hasAccount()).isTrue();
        // 未确认分组的 40 分不得出现：成绩还没公布，不能据此发补考通知
        assertThat(list.items()).extracting(i -> i.studentId()).doesNotContain(3L);
    }

    @Test
    void failedStudentsShouldRejectWhenNoGroupConfirmed() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 100L, "计算机2301班", false)));
        when(scoreMapper.selectList(any())).thenReturn(List.of(score(1L, "30")));

        assertThatThrownBy(() -> service.failedStudents(10L, null, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(3007));
    }

    @Test
    void failedStudentsShouldRejectOthersCourse() {
        assertThatThrownBy(() -> service.failedStudents(10L, null, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(3001));
    }

    @Test
    void sendShouldPersistRecipientsAndCountMissingAccounts() {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 100L, "计算机2301班", true),
                member(2L, 100L, "计算机2301班", true)));
        when(scoreMapper.selectList(any())).thenReturn(List.of(score(1L, "55"), score(2L, "20")));
        when(studentMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                student(1L, "2023010101", "李同学", 9L),
                student(2L, "2023010102", "王同学", null)));
        Mockito.doAnswer(inv -> {
            inv.getArgument(0, Notice.class).setId(99L);
            return 1;
        }).when(noticeMapper).insert(any(Notice.class));

        SendNoticeResult result = service.send(10L,
                new SendNoticeRequest(null, "补考安排", "请于本周五参加补考",
                        LocalDateTime.of(2026, 11, 20, 14, 0), "实验楼302", "第1-6章"),
                1L);

        assertThat(result.noticeId()).isEqualTo(99L);
        assertThat(result.sent()).isEqualTo(2);
        // 2 号没有登录账号：站内信发不达，必须单独计数而不是静默丢弃
        assertThat(result.noAccount()).isEqualTo(1);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getType()).isEqualTo(Notice.TYPE_MAKEUP_EXAM);
        assertThat(noticeCaptor.getValue().getExamPlace()).isEqualTo("实验楼302");
        assertThat(noticeCaptor.getValue().getRevoked()).isFalse();

        ArgumentCaptor<NoticeRecipient> recipientCaptor = ArgumentCaptor.forClass(NoticeRecipient.class);
        verify(recipientMapper, Mockito.times(2)).insert(recipientCaptor.capture());
        assertThat(recipientCaptor.getAllValues())
                .extracting(NoticeRecipient::getStudentId)
                .containsExactlyInAnyOrder(1L, 2L);
        // 分数快照必须落下来，事后才能解释「为什么发给他」
        assertThat(recipientCaptor.getAllValues())
                .extracting(NoticeRecipient::getScoreSnapshot)
                .containsExactlyInAnyOrder(new BigDecimal("55"), new BigDecimal("20"));
    }

    @Test
    void sendShouldRejectEmptyFailedList() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 100L, "计算机2301班", true)));
        when(scoreMapper.selectList(any())).thenReturn(List.of(score(1L, "88")));
        when(studentMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                student(1L, "2023010101", "李同学", 9L)));

        assertThatThrownBy(() -> service.send(10L,
                new SendNoticeRequest(null, "补考安排", "内容", null, null, null), 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(4001));
        verify(noticeMapper, never()).insert(any(Notice.class));
    }

    @Test
    void myNoticesShouldFilterByOwnStudentAndIgnoreRevoked() {
        Student me = student(1L, "2023010101", "李同学", 9L);
        when(studentMapper.selectOne(any())).thenReturn(me);

        NoticeRecipient unread = new NoticeRecipient();
        unread.setId(501L);
        unread.setNoticeId(99L);
        unread.setStudentId(1L);
        NoticeRecipient read = new NoticeRecipient();
        read.setId(502L);
        read.setNoticeId(98L);
        read.setStudentId(1L);
        read.setReadAt(LocalDateTime.now().minusHours(1));
        when(recipientMapper.selectList(any())).thenReturn(List.of(unread, read));

        Notice shown = new Notice();
        shown.setId(99L);
        shown.setCourseId(10L);
        shown.setTitle("补考安排");
        shown.setCreatedAt(LocalDateTime.now());
        shown.setRevoked(false);
        Notice revoked = new Notice();
        revoked.setId(98L);
        revoked.setCourseId(10L);
        revoked.setTitle("已撤回的通知");
        revoked.setCreatedAt(LocalDateTime.now());
        revoked.setRevoked(true);
        when(noticeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(shown, revoked));
        when(courseMapper.selectBatchIds(anyCollection())).thenReturn(List.of(course));

        MyNoticePage page = service.myNotices(9L);

        // 撤回的那条既不在列表、也不计入未读（否则红点永远消不掉）
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).id()).isEqualTo(501L);
        assertThat(page.items().get(0).courseName()).isEqualTo("Java 程序设计");
        assertThat(page.unreadCount()).isEqualTo(1);
    }

    @Test
    void markReadShouldRejectOthersRecipientRow() {
        when(studentMapper.selectOne(any())).thenReturn(student(1L, "2023010101", "李同学", 9L));
        NoticeRecipient others = new NoticeRecipient();
        others.setId(600L);
        others.setStudentId(2L);
        when(recipientMapper.selectById(600L)).thenReturn(others);

        assertThatThrownBy(() -> service.markRead(9L, 600L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(3011));
        verify(recipientMapper, never()).updateById(any(NoticeRecipient.class));
    }

    @Test
    void markReadShouldBeIdempotent() {
        when(studentMapper.selectOne(any())).thenReturn(student(1L, "2023010101", "李同学", 9L));
        NoticeRecipient already = new NoticeRecipient();
        already.setId(601L);
        already.setStudentId(1L);
        already.setReadAt(LocalDateTime.now().minusMinutes(5));
        when(recipientMapper.selectById(601L)).thenReturn(already);

        service.markRead(9L, 601L);

        verify(recipientMapper, never()).updateById(any(NoticeRecipient.class));
    }

    @Test
    void revokeShouldRejectNoticeOfAnotherCourse() {
        Notice other = new Notice();
        other.setId(77L);
        other.setCourseId(20L);
        when(noticeMapper.selectById(77L)).thenReturn(other);

        assertThatThrownBy(() -> service.revoke(10L, 77L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(3011));
    }
}
