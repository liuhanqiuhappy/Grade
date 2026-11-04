package com.example.grademanager.academic.dto;

/**
 * C3 重命名班级请求（契约 §5-C3，部分更新）。
 * 未提供的字段保持不变；两个都不传由服务层判 4001。
 * gradeYear 需要区分「未传」与「显式 null（清空年级）」，用 provided 标记：
 * Jackson 对显式 null 也会调用 setter，据此记录该键是否出现在请求体里。
 */
public class ClassUpdateRequest {

    private String className;

    private Integer gradeYear;

    private boolean gradeYearProvided;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getGradeYear() {
        return gradeYear;
    }

    public void setGradeYear(Integer gradeYear) {
        this.gradeYear = gradeYear;
        this.gradeYearProvided = true;
    }

    public boolean isGradeYearProvided() {
        return gradeYearProvided;
    }
}
