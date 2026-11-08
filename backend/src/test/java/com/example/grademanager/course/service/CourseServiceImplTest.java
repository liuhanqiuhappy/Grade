package com.example.grademanager.course.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.dto.CourseRequest;
import com.example.grademanager.course.dto.CourseView;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.entity.Student;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 课程单测：建课归属 / 删除顺序（外键安全）/ 已确认只读。 */
class CourseServiceImplTest {

    private CourseMapper courseMapper;
    private CourseMemberMapper memberMapper;
    private ScoreMapper scoreMapper;
    private UserMapper userMapper;
    private CourseServiceImpl service;

    private Course course;

    @BeforeAll
    static void initEntityMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Course.class);
        TableInfoHelper.initTableInfo(assistant, CourseMember.class);
        TableInfoHelper.initTableInfo(assistant, Score.class);
        TableInfoHelper.initTableInfo(assistant, Student.class);
    }

    @BeforeEach
    void setUp() {
        courseMapper = Mockito.mock(CourseMapper.class);
        memberMapper = Mockito.mock(CourseMemberMapper.class);
        scoreMapper = Mockito.mock(ScoreMapper.class);
        userMapper = Mockito.mock(UserMapper.class);
        service = new CourseServiceImpl(courseMapper, memberMapper, userMapper, scoreMapper);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        course = new Course();
        course.setId(10L);
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(1L);
        course.setStatus(Course.STATUS_DRAFT);
        when(courseMapper.selectById(10L)).thenReturn(course);
    }

    @Test
    void createShouldAlwaysWriteTeacherIdFromSession() {
        when(courseMapper.selectOne(any())).thenReturn(null);
        when(courseMapper.insert(any(Course.class))).thenReturn(1);
        when(memberMapper.selectCount(any())).thenReturn(0L);

        CourseView view = service.create(new CourseRequest("Java 程序设计", "2026-2027-1"), 1L);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).insert(captor.capture());
        // teacher_id 是 NOT NULL，漏写会直接 500，这里做回归保护
        assertThat(captor.getValue().getTeacherId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(Course.STATUS_DRAFT);
        assertThat(view.name()).isEqualTo("Java 程序设计");
    }

    @Test
    void deleteShouldClearScoresBeforeRemovingCourse() {
        service.delete(10L, 1L);

        // score 与 course_member 都有指向 course 的外键且未级联：先删子表再删主表，
        // 顺序错了就会撞外键、以 5000 收场
        InOrder order = inOrder(scoreMapper, memberMapper, courseMapper);
        order.verify(scoreMapper).delete(any());
        order.verify(memberMapper).delete(any());
        order.verify(courseMapper).deleteById(10L);
    }

    @Test
    void deleteShouldRejectConfirmedCourse() {
        course.setStatus(Course.STATUS_CONFIRMED);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3003));

        verify(scoreMapper, never()).delete(any());
        verify(courseMapper, never()).deleteById(any(Serializable.class));
    }
}
