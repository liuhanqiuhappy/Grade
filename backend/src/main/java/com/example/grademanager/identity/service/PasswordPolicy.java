package com.example.grademanager.identity.service;

import com.example.grademanager.common.BusinessException;
import com.example.grademanager.common.ErrorCode;

/**
 * 口令强度统一校验器：新建学生 / 自助改密 / 教师重置共用同一套规则，
 * 避免三处各写一套导致规则漂移（改了一处漏两处）。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;

    private PasswordPolicy() {
    }

    /**
     * 校验口令是否满足强度要求，不合规抛 4001，message 指明字段与原因。
     *
     * @param raw       明文口令
     * @param fieldName 提示用的字段名（如 newPassword）
     */
    public static void validate(String raw, String fieldName) {
        String field = (fieldName == null || fieldName.isBlank()) ? "newPassword" : fieldName;
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, field + ": 口令不能为空");
        }
        if (raw.length() < MIN_LENGTH || raw.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PARAM,
                    field + ": 口令长度需为 " + MIN_LENGTH + "~" + MAX_LENGTH + " 位");
        }
        if (raw.chars().anyMatch(Character::isWhitespace)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, field + ": 口令不能包含空格");
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, field + ": 口令需同时包含字母和数字");
        }
    }
}
