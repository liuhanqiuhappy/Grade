package com.example.grademanager.score.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.score.dto.CourseStatistics;
import com.example.grademanager.score.dto.SaveScoresResult;
import com.example.grademanager.score.dto.ScoreEntry;
import com.example.grademanager.score.dto.ScoreEntryRequest;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.score.service.impl.ScoreServiceImpl;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 成绩模块单测：归属校验 / 录入校验与并发冲突 / 分组确认 / 学生端未公布口径。 */
class ScoreServiceImplTest {

    private ScoreMapper scoreMapper;
    private CourseMapper courseMapper;
    private CourseMemberMapper memberMapper;
    private StudentMapper studentMapper;
    private UserMapper userMapper;
    private ScoreServiceImpl service;

    private Course course;

    @BeforeAll
    static void initEntityMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Score.class);
        TableInfoHelper.initTableInfo(assistant, CourseMember.class);
        TableInfoHelper.initTableInfo(assistant, Student.class);
        TableInfoHelper.initTableInfo(assistant, Course.class);
    }

    @BeforeEach
    void setUp() {
        scoreMapper = Mockito.mock(ScoreMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        memberMapper = Mockito.mock(CourseMemberMapper.class);
        studentMapper = Mockito.mock(StudentMapper.class);
        userMapper = Mockito.mock(UserMapper.class);
        CourseServiceImpl courseService = new CourseServiceImpl(courseMapper, memberMapper, userMapper, scoreMapper);
        service = new ScoreServiceImpl(scoreMapper, courseMapper, memberMapper, studentMapper, userMapper, courseService);

        course = new Course();
        course.setId(10L);
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(1L);
        course.setStatus(Course.STATUS_DRAFT);
        when(courseMapper.selectById(10L)).thenReturn(course);
    }

    private CourseMember member(long id, long studentId, long classId, String className, boolean confirmed) {
        CourseMember m = new CourseMember();
        m.setId(id);
        m.setCourseId(10L);
        m.setStudentId(studentId);
        m.setClassId(classId);
        m.setClassName(className);
        m.setGroupConfirmed(confirmed);
        return m;
    }

    private Student student(long id, String no, String name) {
        Student s = new Student();
        s.setId(id);
        s.setStudentNo(no);
        s.setName(name);
        s.setClassId(100L);
        return s;
    }

    private Score scoreOf(long studentId, String value) {
        Score s = new Score();
        s.setCourseId(10L);
        s.setStudentId(studentId);
        s.setScore(new BigDecimal(value));
        return s;
    }

    @Test
    void listScoresShouldRejectOtherTeachersCourseWith3001() {
        course.setTeacherId(2L);
        assertThatThrownBy(() -> service.listScores(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3001));
    }

    @Test
    void saveScoresShouldRejectNonMemberWith3005() {
        when(memberMapper.selectList(any())).thenReturn(List.of());
        ScoreEntryRequest request = new ScoreEntryRequest(List.of(new ScoreEntry(99L, new BigDecimal("88"), null)));
        assertThatThrownBy(() -> service.saveScores(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3005));
    }

    @Test
    void saveScoresShouldRejectOutOfRangeWith3006() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        ScoreEntryRequest request = new ScoreEntryRequest(List.of(new ScoreEntry(5L, new BigDecimal("120"), null)));
        assertThatThrownBy(() -> service.saveScores(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3006));
    }

    @Test
    void saveScoresShouldSkipConfirmedGroupRowsAndSaveOthers() {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 5L, 100L, "计2301", true),
                member(2L, 6L, 200L, "计2302", false)));
        when(studentMapper.selectBatchIds(any()))
                .thenReturn(List.of(student(5L, "2023010101", "李同学"), student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectOne(any())).thenReturn(null);
        when(scoreMapper.insert(any(Score.class))).thenReturn(1);

        ScoreEntryRequest request = new ScoreEntryRequest(List.of(
                new ScoreEntry(5L, new BigDecimal("88"), null),
                new ScoreEntry(6L, new BigDecimal("92"), null)));
        SaveScoresResult result = service.saveScores(10L, request, 1L);

        // 已确认分组的行被跳过并点名，同批其他分组行正常保存，不再抛 3003 中断整批（H1）
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.skippedStudents()).hasSize(1);
        assertThat(result.skippedStudents().get(0)).contains("李同学");
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insert(captor.capture());
        assertThat(captor.getValue().getStudentId()).isEqualTo(6L);
    }

    @Test
    void saveScoresShouldSkipAllWhenEveryRowConfirmed() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", true)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(student(5L, "2023010101", "李同学")));

        ScoreEntryRequest request = new ScoreEntryRequest(List.of(new ScoreEntry(5L, new BigDecimal("88"), null)));
        SaveScoresResult result = service.saveScores(10L, request, 1L);

        assertThat(result.saved()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(scoreMapper, never()).insert(any(Score.class));
        verify(scoreMapper, never()).updateById(any(Score.class));
    }

    @Test
    void saveScoresShouldRejectStaleUpdateWith3010InsteadOfOverwrite() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        Score existing = scoreOf(5L, "70");
        existing.setId(7L);
        existing.setVersion(3);
        when(scoreMapper.selectOne(any())).thenReturn(existing);
        when(scoreMapper.updateById(any(Score.class))).thenReturn(0);

        ScoreEntryRequest request = new ScoreEntryRequest(List.of(new ScoreEntry(5L, new BigDecimal("88"), null)));
        assertThatThrownBy(() -> service.saveScores(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3010));
    }

    @Test
    void saveScoresShouldRejectStaleExpectedVersionWith3010AndNameTheStudent() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(student(5L, "2023010101", "李同学")));
        Score existing = scoreOf(5L, "70");
        existing.setId(7L);
        existing.setVersion(3);
        when(scoreMapper.selectOne(any())).thenReturn(existing);

        // 客户端读到的版本是 2，库里已是 3：说明期间他人保存过，必须拒绝覆盖
        ScoreEntryRequest request = new ScoreEntryRequest(
                List.of(new ScoreEntry(5L, new BigDecimal("88"), 2)));
        assertThatThrownBy(() -> service.saveScores(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(3010);
                    assertThat(e.getMessage()).contains("李同学");
                });
        verify(scoreMapper, never()).updateById(any(Score.class));
    }

    @Test
    void saveScoresShouldAcceptWhenExpectedVersionMatches() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(student(5L, "2023010101", "李同学")));
        Score existing = scoreOf(5L, "70");
        existing.setId(7L);
        existing.setVersion(3);
        when(scoreMapper.selectOne(any())).thenReturn(existing);
        when(scoreMapper.updateById(any(Score.class))).thenReturn(1);

        ScoreEntryRequest request = new ScoreEntryRequest(
                List.of(new ScoreEntry(5L, new BigDecimal("88"), 3)));
        assertThat(service.saveScores(10L, request, 1L).saved()).isEqualTo(1);
    }

    @Test
    void saveScoresShouldInsertWhenAbsent() {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        when(scoreMapper.selectOne(any())).thenReturn(null);
        when(scoreMapper.insert(any(Score.class))).thenReturn(1);

        ScoreEntryRequest request = new ScoreEntryRequest(List.of(new ScoreEntry(5L, new BigDecimal("88"), null)));
        int saved = service.saveScores(10L, request, 1L).saved();

        assertThat(saved).isEqualTo(1);
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insert(captor.capture());
        assertThat(captor.getValue().getScore()).isEqualByComparingTo("88");
        assertThat(captor.getValue().getVersion()).isZero();
    }

    @Test
    void confirmShouldRejectWhenTargetGroupMemberMissingScoreWith3008() {
        when(memberMapper.selectList(any()))
                .thenReturn(List.of(member(1L, 5L, 100L, "计2301", false), member(2L, 6L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(student(5L, "2023010101", "李同学"), student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "90")));

        assertThatThrownBy(() -> service.confirm(10L, 100L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(3008);
                    assertThat(e.getMessage()).contains("王同学");
                });
    }

    @Test
    void confirmShouldOnlyLockTargetGroupAndKeepOtherGroupsUnconfirmed() {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 5L, 100L, "计2301", false),
                member(2L, 6L, 100L, "计2301", false),
                member(3L, 7L, 200L, "计2302", false)));
        when(studentMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(student(5L, "2023010101", "李同学"), student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "90"), scoreOf(6L, "80")));

        service.confirm(10L, 100L, 1L);

        verify(memberMapper).update(any(), any());
        // 计2302 组仍有未确认成员，课程不能整体 CONFIRMED
        verify(courseMapper, never()).updateById(any(Course.class));
    }

    @Test
    void confirmWithoutClassIdShouldConfirmAllAndMarkCourseConfirmed() {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 5L, 100L, "计2301", false),
                member(2L, 6L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(student(5L, "2023010101", "李同学"), student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "90"), scoreOf(6L, "80")));

        service.confirm(10L, null, 1L);

        verify(memberMapper).update(any(), any());
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Course.STATUS_CONFIRMED);
    }

    @Test
    void statisticsShouldExposeClassIdPerGroupToKeepConfirmGranularity() {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(1L, 5L, 100L, "计2301", false),
                member(2L, 6L, 200L, "计2302", false)));
        when(studentMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(student(5L, "2023010101", "李同学"), student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "90")));

        CourseStatistics stats = service.statistics(10L, 1L);

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.unrecorded()).isEqualTo(1);
        // 前端按组确认必须回传 classId；缺了会退化成「确认全部分组」
        assertThat(stats.groups()).extracting(CourseStatistics.GroupStat::classId)
                .containsExactly(100L, 200L);
        assertThat(stats.groups()).extracting(CourseStatistics.GroupStat::className)
                .containsExactly("计2301", "计2302");
    }

    @Test
    void myScoresShouldHideScoresOfUnconfirmedGroup() {
        Student me = student(5L, "2023010101", "李同学");
        me.setUserId(9L);
        when(studentMapper.selectOne(any())).thenReturn(me);
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", false)));
        when(courseMapper.selectBatchIds(anyCollection())).thenReturn(List.of(course));

        User teacher = new User();
        teacher.setId(1L);
        teacher.setDisplayName("张老师");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(teacher));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "95")));

        var views = service.myScores(9L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).published()).isFalse();
        assertThat(views.get(0).score()).isNull();
    }

    @Test
    void myScoresShouldReturnScoreForConfirmedGroup() {
        Student me = student(5L, "2023010101", "李同学");
        me.setUserId(9L);
        when(studentMapper.selectOne(any())).thenReturn(me);
        when(memberMapper.selectList(any())).thenReturn(List.of(member(1L, 5L, 100L, "计2301", true)));
        when(courseMapper.selectBatchIds(anyCollection())).thenReturn(List.of(course));

        User teacher = new User();
        teacher.setId(1L);
        teacher.setDisplayName("张老师");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(teacher));
        when(scoreMapper.selectList(any())).thenReturn(List.of(scoreOf(5L, "95")));

        var views = service.myScores(9L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).published()).isTrue();
        assertThat(views.get(0).score()).isEqualByComparingTo("95");
    }
}
