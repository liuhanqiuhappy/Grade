package com.example.grademanager.academic.dto;

/**
 * S5 修改学生请求（契约 §5-S5，部分更新；一个都不传 → 4001）。
 * 契约外的多余字段（username/password/role）不在本 DTO 中，服务端自动忽略、不报错。
 */
public class StudentUpdateRequest {

    private String name;

    private String studentNo;

    private Long classId;

    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
