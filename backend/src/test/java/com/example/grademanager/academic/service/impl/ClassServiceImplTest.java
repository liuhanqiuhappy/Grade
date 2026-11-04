package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.grademanager.academic.dto.ArchiveClassResponse;
import com.example.grademanager.academic.dto.ClassCreateRequest;
import com.example.grademanager.academic.dto.ClassResponse;
import com.example.grademanager.academic.dto.ClassUpdateRequest;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.GradeGuard;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.identity.session.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 班级服务单测（覆盖契约 §10.1 中 T-C2/C3/C4 系列）。
 * mock Mapper，不依赖 MySQL，便于随时回归。
 */
class ClassServiceImplTest {

    private SchoolClassMapper classMapper;
    private StudentMapper studentMapper;
    private GradeGuard gradeGuard;
    private ClassServiceImpl service;

    private final SessionUser teacher = new SessionUser(1L, "teacher01", "周老师", "TEACHER");

    @BeforeEach
    void setUp() {
        classMapper = mock(SchoolClassMapper.class);
        studentMapper = mock(StudentMapper.class);
        gradeGuard = mock(GradeGuard.class);
        service = new ClassServiceImpl(classMapper, studentMapper, gradeGuard);
    }

    @Test
    void createDuplicateNormalClassIs2001WithClassNameInMessage() {
        when(classMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new ClassCreateRequest("计算机2301班", 2023), teacher));
        assertEquals(2001, ex.getCode());
        assertTrue(ex.getMessage().contains("计算机2301班"), "message 必须指明冲突的班级名");
    }

    @Test
    void createWhitespaceNameIs4001() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new ClassCreateRequest("   ", 2023), teacher));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void createNameTooLongIs4001() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.create(new ClassCreateRequest("班".repeat(65), 2023), teacher));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void createAfterArchiveSameNameSucceeds() {
        // 归档后的同名班级不会挡住新建（唯一索引含 status，服务层只查 status=1）
        when(classMapper.selectCount(any())).thenReturn(0L);
        when(classMapper.insert(any(SchoolClass.class))).thenAnswer(inv -> {
            SchoolClass c = inv.getArgument(0);
            c.setId(3L);
            return 1;
        });
        when(studentMapper.selectCount(any())).thenReturn(0L);

        ClassResponse resp = service.create(new ClassCreateRequest("计算机2301班", 2023), teacher);

        assertEquals(3L, resp.classId());
        assertEquals("计算机2301班", resp.className());
        assertEquals(2023, resp.gradeYear());
        assertEquals(0, resp.studentCount());
        assertEquals(1, resp.status());
        assertNotNull(resp.createdAt());
        ArgumentCaptor<SchoolClass> captor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(classMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getCreatedBy(), "created_by 取当前教师 userId（审计）");
    }

    @Test
    void renameOnlyClassNameKeepsGradeYear() {
        SchoolClass existing = clazz(3L, "计算机2301班", 2023, 1);
        when(classMapper.selectById(3L)).thenReturn(existing);
        when(classMapper.selectCount(any())).thenReturn(0L);
        when(studentMapper.selectCount(any())).thenReturn(0L);

        ClassUpdateRequest request = new ClassUpdateRequest();
        request.setClassName("计算机2302班");
        ClassResponse resp = service.rename(3L, request);

        assertEquals("计算机2302班", existing.getName(), "同一实例应被更新");
        assertEquals(2023, resp.gradeYear(), "未传的 gradeYear 保持不变");
        verify(classMapper).updateById(existing);
    }

    @Test
    void renameNothingProvidedIs4001() {
        ClassUpdateRequest request = new ClassUpdateRequest();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.rename(3L, request));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void renameArchivedClassIs2002() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 2023, 0));
        ClassUpdateRequest request = new ClassUpdateRequest();
        request.setClassName("计算机2302班");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.rename(3L, request));
        assertEquals(2002, ex.getCode());
    }

    @Test
    void renameToDuplicatedNameIs2001() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 2023, 1));
        when(classMapper.selectCount(any())).thenReturn(1L);

        ClassUpdateRequest request = new ClassUpdateRequest();
        request.setClassName("计算机2302班");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.rename(3L, request));
        assertEquals(2001, ex.getCode());
    }

    @Test
    void archiveSuccessReturnsAffectedStudentCountAndKeepsRows() {
        SchoolClass existing = clazz(3L, "计算机2301班", 2023, 1);
        when(classMapper.selectById(3L)).thenReturn(existing);
        when(classMapper.selectCount(any())).thenReturn(0L);
        when(studentMapper.selectCount(any())).thenReturn(42L);

        ArchiveClassResponse resp = service.archive(3L);

        assertEquals(42, resp.affectedStudentCount());
        assertEquals("计算机2301班", resp.className());
        assertNotNull(resp.archivedAt());
        assertEquals(0, existing.getStatus(), "软删除：置 status=0");
        assertNotNull(existing.getArchivedAt());
        verify(classMapper).updateById(existing);
        verify(classMapper, never()).delete(ArgumentMatchers.<Wrapper<SchoolClass>>any());
        // 学生档案行不动：没有针对 student 的任何删除
        // deleteById 有 (Serializable) 与 (T) 两个重载，bare any() 会产生二义性，显式给类型
        verify(studentMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void archiveDeletesOldArchivedRowWithSameNameGrade() {
        // 同 (name, grade_year) 已有一条 status=0 归档记录 → 先物理删除旧归档行，否则撞唯一索引
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 2023, 1));
        when(classMapper.selectCount(any())).thenReturn(1L);
        when(studentMapper.selectCount(any())).thenReturn(0L);

        service.archive(3L);

        verify(classMapper).delete(any(Wrapper.class));
    }

    @Test
    void archiveTwiceIs2002() {
        when(classMapper.selectById(3L)).thenReturn(clazz(3L, "计算机2301班", 2023, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.archive(3L));
        assertEquals(2002, ex.getCode(), "重复归档返回 2002，不幂等成功");
    }

    @Test
    void archiveMissingClassIs2002() {
        when(classMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.archive(99L));
        assertEquals(2002, ex.getCode());
    }

    @Test
    void listPaginationParamsClampAndValidate() {
        when(classMapper.selectCount(any())).thenReturn(3L);
        when(classMapper.selectList(any())).thenReturn(java.util.List.of());

        // size=200 → 按 100 处理
        var clamped = service.list(null, null, null, 1, 200);
        assertEquals(100, clamped.size());

        BusinessException pageInvalid = assertThrows(BusinessException.class,
                () -> service.list(null, null, null, 0, 10));
        assertEquals(4001, pageInvalid.getCode());
        assertEquals("page 必须 ≥1", pageInvalid.getMessage());

        BusinessException sizeInvalid = assertThrows(BusinessException.class,
                () -> service.list(null, null, null, 1, 0));
        assertEquals(4001, sizeInvalid.getCode());
        assertEquals("size 必须在 1–100 之间", sizeInvalid.getMessage());

        BusinessException keywordTooLong = assertThrows(BusinessException.class,
                () -> service.list("k".repeat(65), null, null, 1, 10));
        assertEquals(4001, keywordTooLong.getCode());

        BusinessException badStatus = assertThrows(BusinessException.class,
                () -> service.list(null, null, 2, 1, 10));
        assertEquals(4001, badStatus.getCode());

        BusinessException badGradeYear = assertThrows(BusinessException.class,
                () -> service.list(null, 1999, null, 1, 10));
        assertEquals(4001, badGradeYear.getCode());
    }

    private SchoolClass clazz(Long id, String name, Integer gradeYear, int status) {
        SchoolClass c = new SchoolClass();
        c.setId(id);
        c.setName(name);
        c.setGradeYear(gradeYear);
        c.setStatus(status);
        c.setCreatedAt(LocalDateTime.of(2026, 11, 3, 20, 11, 2));
        return c;
    }
}
