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
import com.example.grademanager.student.dto.ResetPasswordResult;
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

    /* ============ 教师重置学生口令 ============ */

    @Test
    void newStudentAccountShouldRequirePasswordChangeOnFirstLogin() {
        when(userMapper.findByUsername("2023010101")).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        service.create(new StudentRequest("2023010101", "李同学", "男", 100L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getMustChangePassword()).isTrue();
    }

    @Test
    void resetPasswordShouldReturnTempPasswordAndForceChangeNextLogin() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, 9L));
        User account = new User();
        account.setId(9L);
        account.setUsername("2023010101");
        account.setRole(User.ROLE_STUDENT);
        when(userMapper.selectById(9L)).thenReturn(account);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$temp");

        ResetPasswordResult result = service.resetPassword(5L, 1L);

        assertThat(result.studentNo()).isEqualTo("2023010101");
        assertThat(result.temporaryPassword()).hasSize(10);
        assertThat(account.getPasswordHash()).isEqualTo("$2a$10$temp");
        assertThat(account.getMustChangePassword()).isTrue();
        assertThat(account.getPasswordResetBy()).isEqualTo(1L);
        assertThat(account.getPasswordUpdatedAt()).isNotNull();
        verify(userMapper).updateById(account);
    }

    @Test
    void resetPasswordShouldRejectStudentWithoutBoundAccountWith4001() {
        when(studentMapper.selectById(5L)).thenReturn(studentWith(5L, null));

        assertThatThrownBy(() -> service.resetPassword(5L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(4001));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetPasswordShouldRejectUnknownStudentWith2004() {
        when(studentMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(404L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2004));
    }

    /* ============ M4：改学号/姓名同步登录账号 ============ */

    @Test
    void updateShouldSyncAccountUsernameAndDisplayName() {
        Student entity = studentWith(5L, 9L);
        when(studentMapper.selectById(5L)).thenReturn(entity);
        User account = new User();
        account.setId(9L);
        account.setUsername("2023010101");
        account.setDisplayName("李同学");
        account.setRole(User.ROLE_STUDENT);
        when(userMapper.selectById(9L)).thenReturn(account);
        when(userMapper.findByUsername("2023010199")).thenReturn(null);

        service.update(5L, new StudentRequest("2023010199", "李同学改", "男", 100L));

        // 账号即学号：档案改了账号必须跟着改，否则旧学号还能登录、新学号找不到账号
        assertThat(account.getUsername()).isEqualTo("2023010199");
        assertThat(account.getDisplayName()).isEqualTo("李同学改");
        verify(userMapper).updateById(account);
    }

    @Test
    void updateShouldRejectWhenNewStudentNoTakenByAnotherAccountWith2005() {
        Student entity = studentWith(5L, 9L);
        when(studentMapper.selectById(5L)).thenReturn(entity);
        User account = new User();
        account.setId(9L);
        account.setUsername("2023010101");
        account.setRole(User.ROLE_STUDENT);
        when(userMapper.selectById(9L)).thenReturn(account);
        User conflict = new User();
        conflict.setId(10L);
        conflict.setUsername("2023010199");
        when(userMapper.findByUsername("2023010199")).thenReturn(conflict);

        assertThatThrownBy(() -> service.update(5L, new StudentRequest("2023010199", "李同学", "男", 100L)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2005));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateShouldNotTouchTeacherAccountEvenIfBoundByMistake() {
        Student entity = studentWith(5L, 9L);
        when(studentMapper.selectById(5L)).thenReturn(entity);
        User teacher = new User();
        teacher.setId(9L);
        teacher.setUsername("teacher01");
        teacher.setRole(User.ROLE_TEACHER);
        when(userMapper.selectById(9L)).thenReturn(teacher);

        service.update(5L, new StudentRequest("2023010199", "李同学", "男", 100L));

        // 绑定关系异常时不能顺手改教师账号的用户名
        verify(userMapper, never()).updateById(any(User.class));
    }

    /* ============ L2：全部行非法也要给失败清单 ============ */

    @Test
    void importExcelShouldReportFailuresInsteadOfParseErrorWhenEveryRowInvalid() throws Exception {
        when(studentMapper.selectCount(any())).thenReturn(1L); // 两行学号都已存在
        byte[] bytes = xlsxBytes(new String[][]{
                {"学号", "姓名", "性别"},
                {"2023010101", "重复一", "男"},
                {"2023010102", "重复二", "女"}});
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        var result = service.importExcel(100L, file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failures()).hasSize(2);
        assertThat(result.failures()).extracting(r -> r.row()).containsExactly(2, 3);
    }

    @Test
    void importExcelShouldRejectEmptyFileWith2006() throws Exception {
        byte[] bytes = xlsxBytes(new String[][]{{"学号", "姓名", "性别"}});
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> service.importExcel(100L, file))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2006));
    }

    private byte[] xlsxBytes(String[][] rows) throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var out = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("s");
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
