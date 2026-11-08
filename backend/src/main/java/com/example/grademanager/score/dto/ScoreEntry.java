package com.example.grademanager.score.dto;

import java.math.BigDecimal;

/**
 * 单条成绩录入：studentId 必填，score 可空（表示暂不录入）。
 * expectedVersion = 读取该行时的版本号，用于阻止后写覆盖先写（§6-4e）：
 * null 表示该行此前未录入（新行），非 null 必须与库中版本一致，否则拒绝保存并指名到人。
 * 注：新插入行的版本即 0，因此 0 是合法版本值，不能当作「不存在」的哨兵。
 */
public record ScoreEntry(
        @jakarta.validation.constraints.NotNull(message = "studentId 不能为空") Long studentId,
        BigDecimal score,
        Integer expectedVersion) {
}
