package com.example.grademanager.score.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.course.service.impl.CourseServiceImpl;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.notice.mapper.NoticeMapper;
import com.example.grademanager.notice.mapper.NoticeRecipientMapper;
import com.example.grademanager.score.dto.ScoreImportResult;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.score.service.impl.ScoreServiceImpl;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 成绩 Excel 导入：逐行校验、失败行不中断、已确认分组只读、整课确认拒绝。 */
class ScoreImportServiceImplTest {

    private ScoreMapper scoreMapper;
    private CourseMapper courseMapper;
    private CourseMemberMapper memberMapper;
    private StudentMapper studentMapper;
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
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        CourseServiceImpl courseService = new CourseServiceImpl(courseMapper, memberMapper, userMapper, scoreMapper,
                Mockito.mock(NoticeMapper.class), Mockito.mock(NoticeRecipientMapper.class));
        service = new ScoreServiceImpl(scoreMapper, courseMapper, memberMapper, studentMapper, userMapper, courseService);

        course = new Course();
        course.setId(10L);
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(1L);
        course.setStatus(Course.STATUS_DRAFT);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(scoreMapper.insert(any(Score.class))).thenReturn(1);
        when(scoreMapper.updateById(any(Score.class))).thenReturn(1);
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

    private Student student(long id, String no, String name) {
        Student s = new Student();
        s.setId(id);
        s.setStudentNo(no);
        s.setName(name);
        s.setClassId(100L);
        return s;
    }

    /** 第 1 行固定表头；数据行按 [学号, 分数] 成对给出，分数传 null 表示留空单元格。 */
    private MockMultipartFile excel(Object[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("成绩");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("分数");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(rows[i][0]));
                Object value = rows[i][1];
                if (value == null) {
                    continue;
                }
                if (value instanceof Number number) {
                    row.createCell(1).setCellValue(number.doubleValue());
                } else {
                    row.createCell(1).setCellValue(String.valueOf(value));
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "scores.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void importShouldInsertNewRowsAndUpdateExistingOnes() throws Exception {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(5L, 100L, "计2301", false),
                member(6L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(
                student(5L, "2023010101", "李同学"),
                student(6L, "2023010102", "王同学")));
        Score existing = new Score();
        existing.setId(7L);
        existing.setCourseId(10L);
        existing.setStudentId(5L);
        existing.setScore(new BigDecimal("70"));
        existing.setVersion(2);
        when(scoreMapper.selectList(any())).thenReturn(List.of(existing));

        ScoreImportResult result = service.importScores(10L,
                excel(new Object[]{"2023010101", 88.5}, new Object[]{"2023010102", "92"}), 1L);

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failures()).isEmpty();
        verify(scoreMapper).insert(any(Score.class));
        ArgumentCaptor<Score> updated = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).updateById(updated.capture());
        assertThat(updated.getValue().getStudentId()).isEqualTo(5L);
        assertThat(updated.getValue().getScore()).isEqualByComparingTo("88.5");
        // 已有行保留版本号，交给 @Version 自增；导入不写死版本
        assertThat(updated.getValue().getVersion()).isEqualTo(2);
    }

    @Test
    void importShouldReportStudentOutsideMemberListAndKeepOtherRows() throws Exception {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(5L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(student(5L, "2023010101", "李同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of());

        ScoreImportResult result = service.importScores(10L,
                excel(new Object[]{"2023010101", 75}, new Object[]{"2023999999", 80}), 1L);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).row()).isEqualTo(3);
        assertThat(result.failures().get(0).reason()).contains("不在本课程成员名单");
    }

    @Test
    void importShouldReportNonNumericAndOutOfRangeScores() throws Exception {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(5L, 100L, "计2301", false),
                member(6L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(
                student(5L, "2023010101", "李同学"),
                student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of());

        ScoreImportResult result = service.importScores(10L,
                excel(new Object[]{"2023010101", "abc"}, new Object[]{"2023010102", 120}), 1L);

        assertThat(result.successCount()).isZero();
        assertThat(result.failures()).hasSize(2);
        assertThat(result.failures().get(0).reason()).contains("必须是数字");
        assertThat(result.failures().get(1).reason()).contains("0~100");
        verify(scoreMapper, never()).insert(any(Score.class));
    }

    @Test
    void importShouldSkipRowsOfConfirmedGroup() throws Exception {
        when(memberMapper.selectList(any())).thenReturn(List.of(
                member(5L, 100L, "计2301", true),
                member(6L, 200L, "计2302", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(
                student(5L, "2023010101", "李同学"),
                student(6L, "2023010102", "王同学")));
        when(scoreMapper.selectList(any())).thenReturn(List.of());

        ScoreImportResult result = service.importScores(10L,
                excel(new Object[]{"2023010101", 88}, new Object[]{"2023010102", 91}), 1L);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).reason()).contains("已确认发布");
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insert(captor.capture());
        assertThat(captor.getValue().getStudentId()).isEqualTo(6L);
    }

    @Test
    void importShouldRejectConfirmedCourseWith3003() throws Exception {
        course.setStatus(Course.STATUS_CONFIRMED);
        assertThatThrownBy(() -> service.importScores(10L, excel(new Object[]{"2023010101", 80}), 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3003));
    }

    @Test
    void importShouldRejectHeaderOnlyWorkbookWith2006() throws Exception {
        when(memberMapper.selectList(any())).thenReturn(List.of(member(5L, 100L, "计2301", false)));
        when(studentMapper.selectBatchIds(any())).thenReturn(List.of(student(5L, "2023010101", "李同学")));

        assertThatThrownBy(() -> service.importScores(10L, excel(), 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(2006));
    }

    @Test
    void importShouldRejectOtherTeachersCourseWith3001() throws Exception {
        course.setTeacherId(2L);
        assertThatThrownBy(() -> service.importScores(10L, excel(new Object[]{"2023010101", 80}), 1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(3001));
    }
}
