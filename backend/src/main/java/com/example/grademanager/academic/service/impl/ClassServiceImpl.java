package com.example.grademanager.academic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.grademanager.academic.dto.ArchiveClassResponse;
import com.example.grademanager.academic.dto.ClassCreateRequest;
import com.example.grademanager.academic.dto.ClassResponse;
import com.example.grademanager.academic.dto.ClassUpdateRequest;
import com.example.grademanager.academic.dto.PageResult;
import com.example.grademanager.academic.entity.SchoolClass;
import com.example.grademanager.academic.entity.Student;
import com.example.grademanager.academic.mapper.SchoolClassMapper;
import com.example.grademanager.academic.mapper.StudentMapper;
import com.example.grademanager.academic.service.ClassService;
import com.example.grademanager.academic.service.GradeGuard;
import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;
import com.example.grademanager.identity.session.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClassServiceImpl implements ClassService {

    private static final Logger log = LoggerFactory.getLogger(ClassServiceImpl.class);

    /** 契约模块二 §2.1：对外时间统一 yyyy-MM-dd HH:mm:ss 字符串（GMT+8） */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SchoolClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final GradeGuard gradeGuard;

    public ClassServiceImpl(SchoolClassMapper classMapper, StudentMapper studentMapper, GradeGuard gradeGuard) {
        this.classMapper = classMapper;
        this.studentMapper = studentMapper;
        this.gradeGuard = gradeGuard;
    }

    @Override
    public PageResult<ClassResponse> list(String keyword, Integer gradeYear, Integer status, Integer page, Integer size) {
        String kw = normalizeKeyword(keyword);
        int statusVal = normalizeStatus(status);
        if (gradeYear != null && (gradeYear < 2000 || gradeYear > 2100)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "gradeYear 必须在 2000–2100 之间");
        }
        int[] ps = normalizePage(page, size);

        LambdaQueryWrapper<SchoolClass> wrapper = Wrappers.lambdaQuery(SchoolClass.class)
                .like(kw != null, SchoolClass::getName, kw)
                .eq(gradeYear != null, SchoolClass::getGradeYear, gradeYear)
                .eq(SchoolClass::getStatus, statusVal);
        long total = classMapper.selectCount(wrapper);
        // 固定排序：status ASC, created_at DESC（正常班在前，新建的排前面），否则分页会抖
        wrapper.orderByAsc(SchoolClass::getStatus)
                .orderByDesc(SchoolClass::getCreatedAt)
                .last("LIMIT " + (long) (ps[0] - 1) * ps[1] + "," + ps[1]);
        List<SchoolClass> rows = classMapper.selectList(wrapper);

        List<ClassResponse> list = rows.stream()
                .map(c -> toResponse(c, countActiveStudents(c.getId())))
                .toList();
        return PageResult.of(list, total, ps[0], ps[1]);
    }

    @Override
    public ClassResponse create(ClassCreateRequest request, SessionUser operator) {
        String name = normalizeClassName(request.className());
        Integer gradeYear = request.gradeYear();
        ensureNameNotDuplicated(name, gradeYear, null);

        SchoolClass entity = new SchoolClass();
        entity.setName(name);
        entity.setGradeYear(gradeYear);
        entity.setStatus(1);
        // created_by 仅作审计记录，不作权限边界（班级是全局共享目录，契约 §1.3）
        entity.setCreatedBy(operator == null ? null : operator.userId());
        try {
            classMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.warn("新建班级唯一索引冲突：{}", e.getMessage());
            throw toNameDuplicated(name, e);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return toResponse(entity, 0);
    }

    @Override
    public ClassResponse rename(Long classId, ClassUpdateRequest request) {
        boolean classNameProvided = request.getClassName() != null;
        boolean gradeYearProvided = request.isGradeYearProvided();
        if (!classNameProvided && !gradeYearProvided) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "未提供任何可改字段");
        }
        String newName = classNameProvided ? normalizeClassName(request.getClassName()) : null;

        SchoolClass entity = requireActiveClass(classId);
        String finalName = classNameProvided ? newName : entity.getName();
        Integer finalGradeYear = gradeYearProvided ? request.getGradeYear() : entity.getGradeYear();
        if (classNameProvided) {
            ensureNameNotDuplicated(finalName, finalGradeYear, classId);
        }
        entity.setName(finalName);
        entity.setGradeYear(finalGradeYear);
        try {
            classMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            log.warn("重命名班级唯一索引冲突：{}", e.getMessage());
            throw toNameDuplicated(finalName, e);
        }
        return toResponse(entity, countActiveStudents(classId));
    }

    @Override
    @Transactional
    public ArchiveClassResponse archive(Long classId) {
        SchoolClass entity = classMapper.selectById(classId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        if (entity.getStatus() == 0) {
            // 不幂等：重复归档返回 2002，静默成功会掩盖前端误发两次的 bug（契约 §5-C4）
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        // 校验顺序见契约 §6.3：第 3 步模块二恒为 false，模块四只需替换 GradeGuard 实现
        if (gradeGuard.hasDraftGrades(classId)) {
            throw new BusinessException(ErrorCode.CLASS_HAS_DRAFT_GRADES, "该班级存在未发布成绩，请先完成确认或清空后再归档");
        }
        // uk_school_class_name 含 status：同一 (name, grade_year) 只能有一条 status=0 归档记录。
        // 该班无学生引用才会进入重复归档分支之外的场景……不，此处是首次归档，若历史上已有同
        // (name, grade_year) 的归档行（它已无学生引用），必须先物理删除旧行，否则撞唯一索引。
        Long archivedSameName = classMapper.selectCount(Wrappers.lambdaQuery(SchoolClass.class)
                .eq(SchoolClass::getName, entity.getName())
                .eq(entity.getGradeYear() != null, SchoolClass::getGradeYear, entity.getGradeYear())
                .isNull(entity.getGradeYear() == null, SchoolClass::getGradeYear)
                .eq(SchoolClass::getStatus, 0)
                .ne(SchoolClass::getId, classId));
        if (archivedSameName != null && archivedSameName > 0) {
            classMapper.delete(Wrappers.lambdaQuery(SchoolClass.class)
                    .eq(SchoolClass::getName, entity.getName())
                    .eq(entity.getGradeYear() != null, SchoolClass::getGradeYear, entity.getGradeYear())
                    .isNull(entity.getGradeYear() == null, SchoolClass::getGradeYear)
                    .eq(SchoolClass::getStatus, 0)
                    .ne(SchoolClass::getId, classId));
        }

        // 软删除：置 status=0 + archived_at=now。班级行、学生行、student.class_id 全部保留
        long affected = countActiveStudents(classId);
        entity.setStatus(0);
        entity.setArchivedAt(LocalDateTime.now());
        classMapper.updateById(entity);
        log.info("班级已归档 classId={} name={} affectedStudentCount={}", classId, entity.getName(), affected);
        return new ArchiveClassResponse(entity.getId(), entity.getName(),
                TIME_FORMAT.format(entity.getArchivedAt()), affected);
    }

    private SchoolClass requireActiveClass(Long classId) {
        SchoolClass entity = classMapper.selectById(classId);
        if (entity == null || entity.getStatus() == 0) {
            // 归档班不允许再改（契约 §5-C3）
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }
        return entity;
    }

    private void ensureNameNotDuplicated(String name, Integer gradeYear, Long excludeClassId) {
        Long count = classMapper.selectCount(Wrappers.lambdaQuery(SchoolClass.class)
                .eq(SchoolClass::getName, name)
                .eq(gradeYear != null, SchoolClass::getGradeYear, gradeYear)
                .isNull(gradeYear == null, SchoolClass::getGradeYear)
                .eq(SchoolClass::getStatus, 1)
                .ne(excludeClassId != null, SchoolClass::getId, excludeClassId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CLASS_NAME_DUPLICATED, "班级「" + name + "」已存在");
        }
    }

    private BusinessException toNameDuplicated(String name, DuplicateKeyException e) {
        if (String.valueOf(e.getMessage()).contains("uk_school_class_name")) {
            return new BusinessException(ErrorCode.CLASS_NAME_DUPLICATED, "班级「" + name + "」已存在");
        }
        throw e;
    }

    private long countActiveStudents(Long classId) {
        Long count = studentMapper.selectCount(Wrappers.lambdaQuery(Student.class)
                .eq(Student::getClassId, classId)
                .eq(Student::getStatus, 1));
        return count == null ? 0 : count;
    }

    private ClassResponse toResponse(SchoolClass entity, long studentCount) {
        return new ClassResponse(entity.getId(), entity.getName(), entity.getGradeYear(), studentCount,
                entity.getStatus(),
                entity.getArchivedAt() == null ? null : TIME_FORMAT.format(entity.getArchivedAt()),
                entity.getCreatedAt() == null ? null : TIME_FORMAT.format(entity.getCreatedAt()));
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "className 不能为空");
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "className 不能为空");
        }
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "className 去空格后最长 64 个字符");
        }
        return name;
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

    /** page ≥1（从 1 开始）；size 1–100，超过 100 按 100 处理（契约 §2.1） */
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
