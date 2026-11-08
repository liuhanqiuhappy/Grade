package com.example.grademanager.identity.service;

import java.security.SecureRandom;

/**
 * 教师重置口令时生成一次性临时口令：10 位、剔除 0/O/1/l/I 等易混字符、必含字母与数字。
 * 明文只在重置接口的响应里出现一次，库中只存 BCrypt 哈希。
 */
public final class PasswordGenerator {

    private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String ALL = LETTERS + DIGITS;
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String temporaryPassword() {
        char[] chars = new char[LENGTH];
        // 先各放一个字母与数字，保证一定满足 PasswordPolicy 的强度要求
        chars[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 2; i < LENGTH; i++) {
            chars[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        // Fisher-Yates 洗牌，避免字母与数字的位置固定
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
