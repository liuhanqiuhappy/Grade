package com.example.grademanager.student.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.dto.StudentRequest;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import com.example.grademanager.student.service.impl.StudentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 学生模块单测：删除保护（§4-5）与自动建号绑定（§4-4）。 */
class StudentServiceImplTest {

    private StudentMapper studentMapper;
    private SchoolClassMapper classMapper;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private CourseMemberMapper memberMapper;
    private ScoreMapper scoreMapper;
    private StudentServiceImpl service;

    private SchoolClass clazz;

    @BeforeAll
    static void initEntityMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Student.class);
        TableInfoHelper.initTableInfo(assistant, CourseMember.class);
        TableInfoHelper.initTableInfo(assistant, Score.class);
        TableInfoHelper.initTableInfo(assistant, SchoolClass.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        studentMapper = Mockito.mock(StudentMapper.class);
        classMapper = Mockito.mock(SchoolClassMapper.class);
        userMapper = Mockito.mock(UserMapper.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        memberMapper = Mockito.mock(CourseMemberMapper.class);
        scoreMapper = Mockito.mock(ScoreMapper.class);
        service = new StudentServiceImpl(studentMapper, classMapper, userMapper, passwordEncoder,
                memberMapper, scoreMapper);

        clazz = new SchoolClass();
        clazz.setId(100L);
        clazz.setName("计算机2301班");
        when(classMapper.selectById(100L)).thenReturn(clazz);
        when(studentMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.selectCount(any())).thenReturn(0L);
        when(scoreMapper.selectCount(any())).thenReturn(0L);
    }

    private Student studentWith(long id, Long userId) {
        Student s = new Student();
        s.setId(id);
        s.setStudentNo("2023010101");
        s.setName("李同学");
        s.setClassId(100L);
        s.setUserId(userId);
        return s;
    }

    @Test
    void deleteShouldRejectWhenStudentInAnyCourseWith2007() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, 9L));
        when(memberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2007));
        verify(studentMapper, never()).deleteById(5L);
    }

    @Test
    void deleteShouldRejectWhenScoreExistsWith2007() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, 9L));
        when(scoreMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2007));
        verify(studentMapper, never()).deleteById(5L);
    }

    @Test
    void deleteShouldRemoveStudentAndBoundAccountWhenUnused() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, 9L));

        service.delete(5L);

        verify(studentMapper).deleteById(5L);
        verify(userMapper).deleteById(9L);
    }

    @Test
    void deleteShouldKeepOthersWhenStudentHasNoBoundUser() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, null));

        service.delete(5L);

        verify(studentMapper).deleteById(5L);
        verify(userMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void createShouldCreateStudentAccountAndBind() {
        when(userMapper.findByUsername("2023010101")).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        service.create(new StudentRequest("2023010101", "李同学", "男", 100L));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("2023010101");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(User.ROLE_STUDENT);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("李同学");
        verify(studentMapper).updateById(any(Student.class));
    }

    @Test
    void createShouldReuseExistingStudentAccountWithSameUsername() {
        User existing = new User();
        existing.setId(66L);
        existing.setUsername("2023010101");
        existing.setRole(User.ROLE_STUDENT);
        when(userMapper.findByUsername("2023010101")).thenReturn(existing);

        service.create(new StudentRequest("2023010101", "李同学", "男", 100L));

        verify(userMapper, never()).insert(any(User.class));
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(66L);
    }

    @Test
    void createShouldNotBindWhenUsernameTakenByTeacher() {
        User teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("2023010101");
        teacher.setRole(User.ROLE_TEACHER);
        when(userMapper.findByUsername("2023010101")).thenReturn(teacher);

        service.create(new StudentRequest("2023010101", "李同学", "男", 100L));

        verify(userMapper, never()).insert(any(User.class));
        // 与教师账号撞名时不绑定，避免越权关联
        verify(studentMapper, never()).updateById(any(Student.class));
    }
}
