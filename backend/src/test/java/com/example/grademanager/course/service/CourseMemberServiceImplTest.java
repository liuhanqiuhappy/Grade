package com.example.grademanager.course.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.dto.MemberAddResult;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseMemberServiceImpl;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.mapper.UserMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 课程成员单测：班级快照（BR05）/ 归属校验 / 确认分组保护。 */
class CourseMemberServiceImplTest {

    private CourseMapper courseMapper;
    private CourseMemberMapper memberMapper;
    private StudentMapper studentMapper;
    private SchoolClassMapper classMapper;
    private ScoreMapper scoreMapper;
    private CourseMemberServiceImpl service;

    private Course course;

    @BeforeAll
    static void initEntityMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CourseMember.class);
        TableInfoHelper.initTableInfo(assistant, Course.class);
        TableInfoHelper.initTableInfo(assistant, Student.class);
        TableInfoHelper.initTableInfo(assistant, SchoolClass.class);
        TableInfoHelper.initTableInfo(assistant, Score.class);
    }

    @BeforeEach
    void setUp() {
        courseMapper = Mockito.mock(CourseMapper.class);
        memberMapper = Mockito.mock(CourseMemberMapper.class);
        studentMapper = Mockito.mock(StudentMapper.class);
        classMapper = Mockito.mock(SchoolClassMapper.class);
        scoreMapper = Mockito.mock(ScoreMapper.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        CourseServiceImpl courseService = new CourseServiceImpl(courseMapper, memberMapper, userMapper, scoreMapper);
        service = new CourseMemberServiceImpl(courseService, memberMapper, studentMapper, classMapper, scoreMapper);

        course = new Course();
        course.setId(10L);
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(1L);
        course.setStatus(Course.STATUS_DRAFT);
        when(courseMapper.selectById(10L)).thenReturn(course);
    }

    @Test
    void addMembersShouldSnapshotStudentClassAtJoinTime() {
        when(memberMapper.selectList(any())).thenReturn(List.of());
        Student student = new Student();
        student.setId(5L);
        student.setStudentNo("2023010101");
        student.setName("李同学");
        student.setClassId(100L);
        when(studentMapper.selectById(5L)).thenReturn(student);
        SchoolClass clazz = new SchoolClass();
        clazz.setId(100L);
        clazz.setName("计算机2301班");
        when(classMapper.selectById(100L)).thenReturn(clazz);

        MemberAddResult result = service.addMembers(10L, List.of(5L), 1L);

        assertThat(result.added()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        ArgumentCaptor<CourseMember> captor = ArgumentCaptor.forClass(CourseMember.class);
        verify(memberMapper).insert(captor.capture());
        assertThat(captor.getValue().getClassId()).isEqualTo(100L);
        assertThat(captor.getValue().getClassName()).isEqualTo("计算机2301班");
        assertThat(captor.getValue().getGroupConfirmed()).isFalse();
    }

    @Test
    void addMembersShouldSkipExistingMembersAndReportCounts() {
        CourseMember already = new CourseMember();
        already.setId(1L);
        already.setCourseId(10L);
        already.setStudentId(5L);
        when(memberMapper.selectList(any())).thenReturn(List.of(already));

        Student fresh = new Student();
        fresh.setId(6L);
        fresh.setStudentNo("2023010102");
        fresh.setName("王同学");
        fresh.setClassId(100L);
        when(studentMapper.selectById(6L)).thenReturn(fresh);

        // §6-3b：已在课程中的跳过而不是整批失败，实际新增与被跳过人数都要报出来
        MemberAddResult result = service.addMembers(10L, List.of(5L, 6L), 1L);

        assertThat(result.added()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(memberMapper).insert(any(CourseMember.class));
    }

    @Test
    void addMembersShouldRejectWhenCourseConfirmedWith3003() {
        course.setStatus(Course.STATUS_CONFIRMED);
        assertThatThrownBy(() -> service.addMembers(10L, List.of(5L), 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3003));
    }

    @Test
    void listMembersShouldRejectOtherTeachersCourseWith3001() {
        course.setTeacherId(2L);
        assertThatThrownBy(() -> service.listMembers(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3001));
    }

    @Test
    void removeMemberShouldRejectConfirmedGroupWith3003() {
        CourseMember member = new CourseMember();
        member.setId(1L);
        member.setCourseId(10L);
        member.setStudentId(5L);
        member.setGroupConfirmed(true);
        when(memberMapper.selectOne(any())).thenReturn(member);

        assertThatThrownBy(() -> service.removeMember(10L, 5L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3003));
        verify(memberMapper, never()).deleteById(1L);
    }

    @Test
    void removeMemberShouldDeleteMemberAndDraftScore() {
        CourseMember member = new CourseMember();
        member.setId(1L);
        member.setCourseId(10L);
        member.setStudentId(5L);
        member.setGroupConfirmed(false);
        when(memberMapper.selectOne(any())).thenReturn(member);

        service.removeMember(10L, 5L, 1L);

        verify(memberMapper).deleteById(1L);
        verify(scoreMapper).delete(any());
    }
}
