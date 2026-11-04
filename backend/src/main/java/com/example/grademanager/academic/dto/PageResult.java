package com.example.grademanager.academic.dto;

import java.util.List;

/**
 * 分页响应（契约模块二 §2.1，后续模块沿用）。
 * list 无数据时返回 []，不返回 null。
 */
public record PageResult<T>(List<T> list, long total, int page, int size) {

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list == null ? List.of() : list, total, page, size);
    }
}
