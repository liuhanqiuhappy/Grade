package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.dto.ResetPasswordResponse;
import com.example.grademanager.academic.dto.StudentCreateRequest;
import com.example.grademanager.academic.dto.StudentCreateResponse;
import com.example.grademanager.academic.dto.StudentImportResponse;
import com.example.grademanager.academic.dto.StudentUpdateRequest;
import com.example.grademanager.academic.dto.StudentView;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.StudentService;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始口令字符集（契约 §5-S2）：32 字符，已剔除易混淆字符 0 O 1 l I。
     * 数据库只存 BCrypt 哈希，明文只在 S2/S6 成功响应里出现一次。
     */
    static final String PASSWORD_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final int PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String STUDENT_NO_PATTERN = "^[A-Za-z0-9]{4,32}$";

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final SchoolClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;

    /** 自引用（@Lazy 代理）：批量导入逐行调用 S2 create 时走 AOP 事务，实现部分成功 */
    @Autowired
    @Lazy
    private StudentService self;

    public StudentServiceImpl(StudentMapper studentMapper, UserMapper userMapper,
                              SchoolClassMapper classMapper, PasswordEncoder passwordEncoder) {
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.classMapper = classMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<StudentView> listByClass(Long classId, String keyword, Integer status, Integer page, Integer size) {
        // 已归档班级仍可查看名单（历史可查），只有完全不存在才 2002（契约 §5-S1）
        SchoolClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        return pageStudents(Wrappers.lambdaQuery(Student.class).eq(Student::getClassId, classId),
                keyword, normalizeStatus(status), page, size);
    }

    @Override
    public PageResult<StudentView> search(String keyword, Long classId, Integer status, Integer page, Integer size) {
        return pageStudents(Wrappers.lambdaQuery(Student.class)
                        .eq(classId != null, Student::getClassId, classId),
                keyword, normalizeStatus(status), page, size);
    }

    @Override
    @Transactional
    public StudentCreateResponse create(Long classId, StudentCreateRequest request) {
        SchoolClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        if (clazz.getStatus() == 0) {
            throw new BusinessException(ErrorCode.TARGET_CLASS_ARCHIVED);
        }
        String studentNo = request.studentNo() == null ? "" : request.studentNo().trim();
        if (!studentNo.matches(STUDENT_NO_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "studentNo 必须为 4–32 位字母数字，不允许空格与中文");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "name 去空格后须为 1–50 个字符");
        }

        // 2003 与 2007 分开判定（契约 §4.2）：学号看 student 表，登录名看 sys_user 表
        Long noCount = studentMapper.selectCount(Wrappers.lambdaQuery(Student.class)
                .eq(Student::getStudentNo, studentNo));
        if (noCount != null && noCount > 0) {
            throw new BusinessException(ErrorCode.STUDENT_NO_DUPLICATED, "学号 " + studentNo + " 已被占用");
        }
        User existingAccount = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, studentNo));
        if (existingAccount != null) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATED, "登录名 " + studentNo + " 已被占用");
        }

        // 服务端生成初始口令；只存 BCrypt 哈希，任何日志/异常 message 不得包含明文
        String initialPassword = generateInitialPassword();
        User user = new User();
        user.setUsername(studentNo);
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setDisplayName(name);
        user.setRole("STUDENT");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            log.warn("建号 sys_user 唯一索引冲突：{}", e.getMessage());
            throw toUserDuplicated(studentNo, e);
        }

        Student student = new Student();
        student.setUserId(user.getId());
        student.setStudentNo(studentNo);
        student.setClassId(classId);
        student.setStatus(1);
        try {
            studentMapper.insert(student);
        } catch (DuplicateKeyException e) {
            log.warn("建号 student 唯一索引冲突：{}", e.getMessage());
            throw new BusinessException(ErrorCode.STUDENT_NO_DUPLICATED, "学号 " + studentNo + " 已被占用");
            // @Transactional 保证 sys_user 插入随之回滚，不留孤立账号（契约 §5-S2）
        }
        if (student.getCreatedAt() == null) {
            student.setCreatedAt(LocalDateTime.now());
        }
        log.info("建号成功 classId={} studentNo={}", classId, studentNo);
        return new StudentCreateResponse(toView(student, user, clazz), initialPassword);
    }

    @Override
    public StudentView detail(Long studentId) {
        Student student = requireStudent(studentId);
        return toView(student, userMapper.selectById(student.getUserId()),
                classMapper.selectById(student.getClassId()));
    }

    @Override
    @Transactional
    public StudentView update(Long studentId, StudentUpdateRequest request) {
        boolean anyProvided = request.getName() != null || request.getStudentNo() != null
                || request.getClassId() != null || request.getStatus() != null;
        if (!anyProvided) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "未提供任何可改字段");
        }
        Student student = requireStudent(studentId);
        User user = userMapper.selectById(student.getUserId());

        String newName = null;
        if (request.getName() != null) {
            newName = request.getName().trim();
            if (newName.isEmpty() || newName.length() > 50) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "name 去空格后须为 1–50 个字符");
            }
        }
        String newStudentNo = null;
        if (request.getStudentNo() != null) {
            newStudentNo = request.getStudentNo().trim();
            if (!newStudentNo.matches(STUDENT_NO_PATTERN)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        "studentNo 必须为 4–32 位字母数字，不允许空格与中文");
            }
            Long count = studentMapper.selectCount(Wrappers.lambdaQuery(Student.class)
                    .eq(Student::getStudentNo, newStudentNo)
                    .ne(Student::getId, studentId));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.STUDENT_NO_DUPLICATED, "学号 " + newStudentNo + " 已被占用");
            }
            User conflict = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                    .eq(User::getUsername, newStudentNo));
            if (conflict != null && !conflict.getId().equals(student.getUserId())) {
                throw new BusinessException(ErrorCode.USERNAME_DUPLICATED, "登录名 " + newStudentNo + " 已被占用");
            }
        }
        if (request.getClassId() != null) {
            SchoolClass target = classMapper.selectById(request.getClassId());
            if (target == null) {
                throw new BusinessException(ErrorCode.CLASS_NOT_FOUND, "目标班级不存在");
            }
            if (target.getStatus() == 0) {
                throw new BusinessException(ErrorCode.TARGET_CLASS_ARCHIVED);
            }
        }
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "status 只能为 0 或 1");
        }

        if (request.getClassId() != null) {
            student.setClassId(request.getClassId());
        }
        if (newStudentNo != null) {
            // 只改 student_no；sys_user.username 建号后永不修改（契约 §3.4）
            student.setStudentNo(newStudentNo);
        }
        if (request.getStatus() != null) {
            student.setStatus(request.getStatus());
        }
        try {
            studentMapper.updateById(student);
        } catch (DuplicateKeyException e) {
            log.warn("改学号唯一索引冲突：{}", e.getMessage());
            throw new BusinessException(ErrorCode.STUDENT_NO_DUPLICATED, "学号 " + newStudentNo + " 已被占用");
        }
        if (newName != null && user != null) {
            user.setDisplayName(newName);
            userMapper.updateById(user);
        }
        return toView(student, user, classMapper.selectById(student.getClassId()));
    }

    @Override
    public ResetPasswordResponse resetPassword(Long studentId) {
        Student student = requireStudent(studentId);
        User user = userMapper.selectById(student.getUserId());
        if (user == null) {
            // 账号与档案必须同时存在（契约 §3.5），缺失按脏数据处理
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        String initialPassword = generateInitialPassword();
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        userMapper.updateById(user);
        // 不使该学生当前会话失效（契约 §11-Q7），旧口令立即失效
        log.info("重置学生初始口令 studentId={}", studentId);
        return new ResetPasswordResponse(studentId, user.getUsername(), initialPassword,
                TIME_FORMAT.format(LocalDateTime.now()));
    }

    @Override
    public StudentImportResponse importFromExcel(Long classId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        SchoolClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        if (clazz.getStatus() == 0) {
            throw new BusinessException(ErrorCode.TARGET_CLASS_ARCHIVED);
        }

        // 解析 Excel：默认第一行为表头（学号、姓名），从第 2 行起为数据；空行跳过。
        // 逐行独立事务（通过 self 代理调用 S2 create），实现"部分成功"：成功行已提交，失败行收集原因。
        List<StudentImportResponse.Failure> failures = new ArrayList<>();
        int total = 0;
        int success = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "Excel 文件没有工作表");
            }
            for (Row row : sheet) {
                int rowNum = row.getRowNum() + 1; // Excel 行号从 1 计数（含表头）
                if (rowNum == 1) {
                    continue; // 跳过表头
                }
                String studentNo = cellString(row.getCell(0));
                String name = cellString(row.getCell(1));
                if (studentNo.isEmpty() && name.isEmpty()) {
                    continue; // 空行
                }
                total++;
                try {
                    self.create(classId, new StudentCreateRequest(studentNo, name));
                    success++;
                } catch (Exception e) {
                    failures.add(new StudentImportResponse.Failure(rowNum, studentNo, name, e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.warn("批量导入解析失败 classId={} file={}", classId, filename, e);
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Excel 文件读取失败，请确认文件未损坏");
        }
        log.info("批量导入完成 classId={} total={} success={} failed={}", classId, total, success, failures.size());
        return StudentImportResponse.of(total, success, failures);
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        String value;
        switch (cell.getCellType()) {
            case STRING -> value = cell.getStringCellValue();
            case NUMERIC -> value = String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> value = String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> value = cell.getCellFormula();
            default -> value = "";
        }
        return value == null ? "" : value.trim();
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
        return student;
    }

    private PageResult<StudentView> pageStudents(LambdaQueryWrapper<Student> wrapper,
                                                 String keyword, int statusVal, Integer page, Integer size) {
        String kw = normalizeKeyword(keyword);
        wrapper.eq(Student::getStatus, statusVal);
        appendKeywordCondition(wrapper, kw);
        int[] ps = normalizePage(page, size);
        long total = studentMapper.selectCount(wrapper);
        // 固定排序：student_no ASC（契约 §5-S1/S3）
        wrapper.orderByAsc(Student::getStudentNo)
                .last("LIMIT " + (long) (ps[0] - 1) * ps[1] + "," + ps[1]);
        List<Student> rows = studentMapper.selectList(wrapper);

        List<StudentView> list = rows.stream()
                .map(s -> toView(s, userMapper.selectById(s.getUserId()), classMapper.selectById(s.getClassId())))
                .toList();
        return PageResult.of(list, total, ps[0], ps[1]);
    }

    /** keyword 同时模糊匹配 student_no 与姓名（姓名在 sys_user.display_name，需先查出候选 user_id） */
    private void appendKeywordCondition(LambdaQueryWrapper<Student> wrapper, String keyword) {
        if (keyword == null) {
            return;
        }
        List<Long> matchedUserIds = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                        .select(User::getId)
                        .like(User::getDisplayName, keyword))
                .stream().map(User::getId).toList();
        wrapper.and(w -> {
            w.like(Student::getStudentNo, keyword);
            if (!matchedUserIds.isEmpty()) {
                w.or().in(Student::getUserId, matchedUserIds);
            }
        });
    }

    private BusinessException toUserDuplicated(String studentNo, DuplicateKeyException e) {
        if (String.valueOf(e.getMessage()).contains("uk_sys_user_username")) {
            return new BusinessException(ErrorCode.USERNAME_DUPLICATED, "登录名 " + studentNo + " 已被占用");
        }
        throw e;
    }

    private StudentView toView(Student student, User user, SchoolClass clazz) {
        return new StudentView(student.getId(), student.getStudentNo(),
                user == null ? null : user.getDisplayName(),
                user == null ? null : user.getUsername(),
                student.getClassId(),
                clazz == null ? null : clazz.getName(),
                clazz == null ? null : clazz.getStatus(),
                student.getStatus(),
                student.getCreatedAt() == null ? null : TIME_FORMAT.format(student.getCreatedAt()));
    }

    private static String generateInitialPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String kw = keyword.trim();
        if (kw.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "keyword 最长 64 个字符");
        }
        return kw;
    }

    private static int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "status 只能为 0 或 1");
        }
        return status;
    }

    private static int[] normalizePage(Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? 10 : size;
        if (p < 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "page 必须 ≥1");
        }
        if (s < 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "size 必须在 1–100 之间");
        }
        if (s > 100) {
            s = 100;
        }
        return new int[]{p, s};
    }
}
