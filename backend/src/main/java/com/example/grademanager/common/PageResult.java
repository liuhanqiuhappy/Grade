package com.example.grademanager.common;

import java.util.List;

/** 通用分页结果。 */
public record PageResult<T>(long total, long page, long size, List<T> records) {
}
