package com.example.grademanager.academic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 班级表 school_class（契约 §3.2）。删除即归档（软删除），绝不物理删除。
 */
@TableName("school_class")
public class SchoolClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称，去首尾空格后 1-64 字符 */
    private String name;

    /** 年级 2000-2100，允许为空 */
    private Integer gradeYear;

    /** 1=正常 0=已归档 */
    private Integer status;

    /** 归档时间，status=0 时非空 */
    private LocalDateTime archivedAt;

    /** 创建者 sys_user.id（仅审计，不作权限边界） */
    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGradeYear() {
        return gradeYear;
    }

    public void setGradeYear(Integer gradeYear) {
        this.gradeYear = gradeYear;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
