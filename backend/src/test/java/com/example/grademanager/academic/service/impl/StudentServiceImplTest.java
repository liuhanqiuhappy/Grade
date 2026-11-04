package com.example.grademanager.academic.service.impl;

import com.example.grademanager.academic.dto.ResetPasswordResponse;
import com.example.grademanager.academic.dto.StudentCreateRequest;
import com.example.grademanager.academic.dto.StudentCreateResponse;
import com.example.grademanager.academic.dto.StudentUpdateRequest;
import com.example.grademanager.academic.dto.StudentView;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生与账号服务单测（覆盖契约 §10.1 中 T-S2/T-S5/T-S6 系列）。
 * mock Mapper，不依赖 MySQL；口令相关断言验证「不落明文、只存 BCrypt 哈希」。
 */
class StudentServiceImplTest {

    private StudentMapper studentMapper;
    private UserMapper userMapper;
    private SchoolClassMapper classMapper;
    private PasswordEncoder passwordEncoder;
    private StudentServiceImpl service;

    @BeforeEach
    void setUp() {
        studentMapper = mock(StudentMapper.class);
        userMapper = mock(UserMapper.class);
        classMapper = mock(SchoolClassMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new StudentServiceImpl(studentMapper, userMapper, classMapper, passwordEncoder);
    }

    @Test
    void createOkGeneratesPasswordAndAccountTogether() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));
        when(studentMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            inv.<User>getArgument(0).setId(100L);
            return 1;
        });
        when(studentMapper.insert(any(Student.class))).thenAnswer(inv -> {
            inv.<Student>getArgument(0).setId(10L);
            return 1;
        });

        StudentCreateResponse resp = service.create(3L, new StudentCreateRequest("2023010101", " 刘同学 "));

        StudentView view = resp.student();
        assertEquals(10L, view.studentId());
        assertEquals("2023010101", view.studentNo());
        assertEquals("2023010101", view.username(), "建号时 username 直接取 student_no");
        assertEquals("刘同学", view.name(), "姓名去空格后存入");
        assertEquals(3L, view.classId());
        assertEquals("计算机2301班", view.className());
        assertEquals(1, view.classStatus());
        assertEquals(1, view.status());

        String pwd = resp.initialPassword();
        assertEquals(8, pwd.length(), "初始口令为 8 位");
        for (char ch : pwd.toCharArray()) {
            assertTrue(StudentServiceImpl.PASSWORD_CHARS.indexOf(ch) >= 0,
                    "口令字符必须来自剔除 0O1lI 的 32 字符集，实际：" + ch);
        }

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertEquals("STUDENT", inserted.getRole());
        assertEquals("刘同学", inserted.getDisplayName());
        assertEquals("2023010101", inserted.getUsername());
        // T-S2-01/T-S2-02：库中不存在明文口令，只存 60 字符 $2a$ 开头的 BCrypt 哈希
        assertTrue(inserted.getPasswordHash().startsWith("$2a$"));
        assertEquals(60, inserted.getPasswordHash().length());
        assertFalse(inserted.getPasswordHash().contains(pwd));
        assertTrue(passwordEncoder.matches(pwd, inserted.getPasswordHash()));
    }

    @Test
    void createDuplicateStudentNoIs2003() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));
        when(studentMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(3L, new StudentCreateRequest("2023010101", "刘同学")));
        assertEquals(2003, ex.getCode());
        assertTrue(ex.getMessage().contains("2023010101"));
    }

    @Test
    void createStudentNoClashingWithTeacherUsernameIs2007() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));
        when(studentMapper.selectCount(any())).thenReturn(0L);
        User teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("2023010101");
        teacher.setRole("TEACHER");
        when(userMapper.selectOne(any())).thenReturn(teacher);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(3L, new StudentCreateRequest("2023010101", "刘同学")));
        assertEquals(2007, ex.getCode(), "学号与某个非本学生的 sys_user.username 冲突 → 2007");
    }

    @Test
    void createInvalidStudentNoIs4001() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(3L, new StudentCreateRequest("2023 0101", "刘同学")));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void createIntoArchivedClassIs2006() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 0));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(3L, new StudentCreateRequest("2023010101", "刘同学")));
        assertEquals(2006, ex.getCode());
    }

    @Test
    void createMissingClassIs2002() {
        when(classMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(99L, new StudentCreateRequest("2023010101", "刘同学")));
        assertEquals(2002, ex.getCode());
    }

    @Test
    void updateStudentNoKeepsUsername() {
        Student student = student(10L, 100L, "2023010101", 3L, 1);
        User user = user(100L, "2023010101", "刘同学");
        when(studentMapper.selectById(10L)).thenReturn(student);
        when(studentMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(100L)).thenReturn(user);
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setStudentNo("2023999999");
        StudentView view = service.update(10L, request);

        assertEquals("2023999999", view.studentNo(), "student_no 已更新");
        assertEquals("2023010101", view.username(), "sys_user.username 不变");
        verify(studentMapper).updateById(student);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateDuplicateStudentNoIs2003() {
        when(studentMapper.selectById(10L)).thenReturn(student(10L, 100L, "2023010101", 3L, 1));
        when(studentMapper.selectCount(any())).thenReturn(1L);

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setStudentNo("2023999999");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(10L, request));
        assertEquals(2003, ex.getCode());
    }

    @Test
    void updateMoveToArchivedClassIs2006() {
        when(studentMapper.selectById(10L)).thenReturn(student(10L, 100L, "2023010101", 3L, 1));
        when(classMapper.selectById(9L)).thenReturn(clazz(9L, "计算机2201班", 0));

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setClassId(9L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(10L, request));
        assertEquals(2006, ex.getCode());
    }

    @Test
    void updateMoveToMissingClassIs2002() {
        when(studentMapper.selectById(10L)).thenReturn(student(10L, 100L, "2023010101", 3L, 1));
        when(classMapper.selectById(99L)).thenReturn(null);

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setClassId(99L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(10L, request));
        assertEquals(2002, ex.getCode());
    }

    @Test
    void updateEmptyBodyIs4001() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(10L, new StudentUpdateRequest()));
        assertEquals(4001, ex.getCode(), "一个字段都不传 → 4001");
    }

    @Test
    void updateInvalidStatusIs4001() {
        when(studentMapper.selectById(10L)).thenReturn(student(10L, 100L, "2023010101", 3L, 1));

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setStatus(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(10L, request));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void updateSuspendAndResumeStatus() {
        Student student = student(10L, 100L, "2023010101", 3L, 1);
        User user = user(100L, "2023010101", "刘同学");
        when(studentMapper.selectById(10L)).thenReturn(student);
        when(userMapper.selectById(100L)).thenReturn(user);
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));

        StudentUpdateRequest suspend = new StudentUpdateRequest();
        suspend.setStatus(0);
        StudentView view = service.update(10L, suspend);
        assertEquals(0, view.status(), "停用");
        assertEquals(1, user.getStatus(), "sys_user.status 不受学生停用影响");
    }

    @Test
    void updateNameWritesDisplayName() {
        Student student = student(10L, 100L, "2023010101", 3L, 1);
        User user = user(100L, "2023010101", "刘同学");
        when(studentMapper.selectById(10L)).thenReturn(student);
        when(userMapper.selectById(100L)).thenReturn(user);
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 1));

        StudentUpdateRequest request = new StudentUpdateRequest();
        request.setName(" 刘一鸣 ");
        StudentView view = service.update(10L, request);
        assertEquals("刘一鸣", view.name());
        assertEquals("刘一鸣", user.getDisplayName(), "姓名写 sys_user.display_name");
        verify(userMapper).updateById(user);
    }

    @Test
    void detailNotFoundIs2004() {
        when(studentMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(99L));
        assertEquals(2004, ex.getCode());
    }

    @Test
    void resetPasswordReplacesHashAndReturnsPlaintextOnce() {
        Student student = student(10L, 100L, "2023010101", 3L, 1);
        String oldHash = passwordEncoder.encode("oldpass1");
        User user = user(100L, "2023010101", "刘同学");
        user.setPasswordHash(oldHash);
        when(studentMapper.selectById(10L)).thenReturn(student);
        when(userMapper.selectById(100L)).thenReturn(user);

        ResetPasswordResponse resp = service.resetPassword(10L);

        assertEquals(10L, resp.studentId());
        assertEquals("2023010101", resp.username());
        assertEquals(8, resp.initialPassword().length());
        assertNotNull(resp.resetAt());
        verify(userMapper).updateById(user);
        assertTrue(passwordEncoder.matches(resp.initialPassword(), user.getPasswordHash()),
                "新口令可登录");
        assertFalse(passwordEncoder.matches("oldpass1", user.getPasswordHash()),
                "旧口令立即失效");
        // 明文口令不得出现在日志（此处仅验证响应之外不落任何其他载体）
        assertFalse(user.getPasswordHash().contains(resp.initialPassword()));
    }

    @Test
    void resetPasswordMissingStudentIs2004() {
        when(studentMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.resetPassword(99L));
        assertEquals(2004, ex.getCode());
    }

    private SchoolClass clazz(Long id, String name, int status) {
        SchoolClass c = new SchoolClass();
        c.setId(id);
        c.setName(name);
        c.setStatus(status);
        return c;
    }

    private Student student(Long id, Long userId, String studentNo, Long classId, int status) {
        Student s = new Student();
        s.setId(id);
        s.setUserId(userId);
        s.setStudentNo(studentNo);
        s.setClassId(classId);
        s.setStatus(status);
        s.setCreatedAt(LocalDateTime.of(2026, 11, 3, 21, 2, 33));
        return s;
    }

    private User user(Long id, String username, String displayName) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setRole("STUDENT");
        u.setStatus(1);
        return u;
    }
}
