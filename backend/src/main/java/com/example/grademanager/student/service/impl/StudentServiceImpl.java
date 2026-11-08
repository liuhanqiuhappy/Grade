package com.example.grademanager.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.common.PageResult;
import com.example.grademanager.course.entity.CourseMember;
import com.example.grademanager.course.mapper.CourseMemberMapper;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.identity.service.PasswordGenerator;
import com.example.grademanager.score.entity.Score;
import com.example.grademanager.score.mapper.ScoreMapper;
import com.example.grademanager.student.dto.ImportResult;
import com.example.grademanager.student.dto.ResetPasswordResult;
import com.example.grademanager.student.dto.StudentRequest;
import com.example.grademanager.student.dto.StudentView;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import com.example.grademanager.student.service.StudentService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    /** 新学生的初始登录口令（与演示账号一致，教师可在课堂告知后由学生自行保管）。 */
    public static final String DEFAULT_STUDENT_PASSWORD = "pass1234";

    /** 性别白名单：只接受这两值或空，防止把成绩等数字列误当性别导入。 */
    private static final Set<String> VALID_GENDERS = Set.of("男", "女");

    private final StudentMapper studentMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CourseMemberMapper memberMapper;
    private final ScoreMapper scoreMapper;

    public StudentServiceImpl(StudentMapper studentMapper, SchoolClassMapper classMapper,
                              UserMapper userMapper, PasswordEncoder passwordEncoder,
                              CourseMemberMapper memberMapper, ScoreMapper scoreMapper) {
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.memberMapper = memberMapper;
        this.scoreMapper = scoreMapper;
    }

    @Override
    public PageResult<StudentView> listByClass(Long classId, long page, long size, String keyword) {
        requireClass(classId);
        return search(classId, page, size, keyword);
    }

    @Override
    public PageResult<StudentView> search(Long classId, long page, long size, String keyword) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            wrapper.eq(Student::getClassId, classId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Student::getName, kw).or().like(Student::getStudentNo, kw));
        }
        wrapper.orderByAsc(Student::getStudentNo);
        Page<Student> result = studentMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, String> classNames = loadClassNames(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                result.getRecords().stream().map(s -> toView(s, classNames)).toList());
    }

    @Override
    @Transactional
    public StudentView create(StudentRequest request) {
        requireClass(request.classId());
        requireStudentNoUnique(null, request.studentNo());
        Student entity = new Student();
        entity.setStudentNo(request.studentNo().trim());
        entity.setName(request.name().trim());
        entity.setGender(normalizeGender(request.gender()));
        entity.setClassId(request.classId());
        studentMapper.insert(entity);
        bindAccount(entity);
        return toView(entity);
    }

    @Override
    public StudentView update(Long id, StudentRequest request) {
        Student entity = studentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        requireClass(request.classId());
        requireStudentNoUnique(id, request.studentNo());
        entity.setStudentNo(request.studentNo().trim());
        entity.setName(request.name().trim());
        entity.setGender(normalizeGender(request.gender()));
        entity.setClassId(request.classId());
        studentMapper.updateById(entity);
        // 历史数据可能没有绑定账号，补一次绑定
        if (entity.getUserId() == null) {
            bindAccount(entity);
        }
        return toView(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student entity = studentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        // 有课程关系或成绩记录时拒绝删除，避免撞外键报 500（§4-5）
        if (memberMapper.selectCount(new LambdaQueryWrapper<CourseMember>()
                .eq(CourseMember::getStudentId, id)) > 0
                || scoreMapper.selectCount(new LambdaQueryWrapper<Score>()
                .eq(Score::getStudentId, id)) > 0) {
            throw new BusinessException(ErrorCode.STUDENT_IN_USE,
                    "该学生已加入课程或存在成绩记录，请先移出课程成员后再删除");
        }
        studentMapper.deleteById(id);
        if (entity.getUserId() != null) {
            userMapper.deleteById(entity.getUserId());
        }
    }

    @Override
    @Transactional
    public ImportResult importExcel(Long classId, MultipartFile file) {
        requireClass(classId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件格式不支持：仅接受 .xlsx（Excel 2007 及以上），请另存为 .xlsx 后重试");
        }

        List<Student> toInsert = new ArrayList<>();
        List<ImportResult.RowError> failures = new ArrayList<>();
        Set<String> seenNos = new HashSet<>();
        boolean hasDataRow = false;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String no = cellText(row, 0, formatter);
                String name = cellText(row, 1, formatter);
                String gender = cellText(row, 2, formatter);
                int excelRow = i + 1;
                if (no.isBlank() && name.isBlank()) {
                    continue;
                }
                if (no.isBlank() || name.isBlank()) {
                    failures.add(new ImportResult.RowError(excelRow, "学号与姓名均不能为空"));
                    continue;
                }
                if (!seenNos.add(no)) {
                    failures.add(new ImportResult.RowError(excelRow, "文件内学号重复: " + no));
                    continue;
                }
                if (studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, no)) > 0) {
                    failures.add(new ImportResult.RowError(excelRow, "学号已存在: " + no));
                    continue;
                }
                // 性别列做白名单校验：历史上曾把成绩列当性别导入，写出 84、78 这类脏值
                if (!gender.isBlank() && !VALID_GENDERS.contains(gender.trim())) {
                    failures.add(new ImportResult.RowError(excelRow,
                            "性别只能是「男」或「女」，当前值: " + gender));
                    continue;
                }
                hasDataRow = true;
                Student s = new Student();
                s.setStudentNo(no);
                s.setName(name);
                s.setGender(gender.isBlank() ? null : gender.trim());
                s.setClassId(classId);
                toInsert.add(s);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 非 .xlsx 内容、损坏文件、空工作簿等一律按"解析失败"处理，不抛 500
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件解析失败：请确认文件为 .xlsx 且首行为表头（学号 / 姓名 / 性别）");
        }

        if (!hasDataRow) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_FAIL,
                    "文件中没有可导入的数据行：请从第 2 行开始填写学生信息");
        }

        for (Student student : toInsert) {
            studentMapper.insert(student);
            bindAccount(student);
        }
        return new ImportResult(toInsert.size(), failures);
    }

    @Override
    @Transactional
    public ResetPasswordResult resetPassword(Long studentId, Long operatorUserId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        if (student.getUserId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM,
                    "该学生尚未绑定登录账号，无法重置口令：请先编辑一次学生资料以补建账号");
        }
        User user = userMapper.selectById(student.getUserId());
        if (user == null || !User.ROLE_STUDENT.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "该学生绑定的账号异常，无法重置口令");
        }

        String temporaryPassword = PasswordGenerator.temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        // 重置即视作「口令已泄露或已遗忘」，强制下次登录先改密；会话不主动踢，下次登录生效（决策 3）
        user.setMustChangePassword(true);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setPasswordResetBy(operatorUserId);
        userMapper.updateById(user);

        return new ResetPasswordResult(student.getId(), student.getStudentNo(), student.getName(),
                temporaryPassword);
    }

    /**
     * 为学生建立/绑定登录账号（§4-2b）：用户名 = 学号，角色 STUDENT，初始口令 pass1234。
     * 学号与既有 STUDENT 账号同名时直接复用绑定；与教师等账号冲突时不绑定，避免越权关联。
     */
    private void bindAccount(Student entity) {
        User existing = userMapper.findByUsername(entity.getStudentNo());
        if (existing != null) {
            if (User.ROLE_STUDENT.equals(existing.getRole())) {
                entity.setUserId(existing.getId());
                studentMapper.updateById(entity);
            }
            return;
        }
        User user = new User();
        user.setUsername(entity.getStudentNo());
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD));
        user.setDisplayName(entity.getName());
        user.setRole(User.ROLE_STUDENT);
        // 初始口令是全班共用的公开秘密，首次登录必须改掉，否则「只知道学号就能看别人成绩」
        user.setMustChangePassword(true);
        userMapper.insert(user);
        entity.setUserId(user.getId());
        studentMapper.updateById(entity);
    }

    /** 性别归一化 + 白名单校验；空值允许（表示未填写）。 */
    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String value = gender.trim();
        if (!VALID_GENDERS.contains(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "性别只能是「男」或「女」，当前值: " + value);
        }
        return value;
    }

    private void requireClass(Long classId) {        if (classId == null || classMapper.selectById(classId) == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
    }

    private void requireStudentNoUnique(Long excludeId, String studentNo) {
        Student existing = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo.trim()));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.STUDENT_NO_DUP);
        }
    }

    private Map<Long, String> loadClassNames(List<Student> students) {
        Set<Long> ids = students.stream().map(Student::getClassId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return classMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private StudentView toView(Student entity) {
        return toView(entity, Map.of());
    }

    private StudentView toView(Student entity, Map<Long, String> classNames) {
        return new StudentView(entity.getId(), entity.getStudentNo(), entity.getName(),
                entity.getGender(), entity.getClassId(),
                classNames.getOrDefault(entity.getClassId(), ""));
    }

    private String cellText(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return formatter.formatCellValue(cell).trim();
        }
        return formatter.formatCellValue(cell).trim();
    }
}
