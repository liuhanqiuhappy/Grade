package com.example.grademanager.common;

import java.math.BigDecimal;

/**
 * 成绩口径的唯一出处：统计、挂科名单、补考通知都调这里，禁止各处再写一个 60。
 * 空值（未录入）不算不及格，0 分算不及格（需求 §6-6b）。
 */
public final class ScoreRules {

    public static final BigDecimal PASS_LINE = new BigDecimal("60");
    public static final BigDecimal MIN = BigDecimal.ZERO;
    public static final BigDecimal MAX = new BigDecimal("100");

    private ScoreRules() {
    }

    public static boolean isFailed(BigDecimal score) {
        return score != null && score.compareTo(PASS_LINE) < 0;
    }

    public static boolean inRange(BigDecimal score) {
        return score != null && score.compareTo(MIN) >= 0 && score.compareTo(MAX) <= 0;
    }
}
