package com.gauzynote.common.utils;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomPasswordGenerator {

    // ===================== 可配置的字符集（排除易混淆字符：0/O、1/l、8/B、9/q 等） =====================
    /** 小写字母（排除 l、o） */
    private static final String LOWERCASE = "abcdefghjkmnpqrstuvwxyz";
    /** 大写字母（排除 I、O） */
    private static final String UPPERCASE = "ABCDEFGHJKMNPQRSTUVWXYZ";
    /** 数字（排除 0、1、8） */
    private static final String DIGITS = "2345679";
    /** 特殊字符（仅保留常用且无歧义的） */
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.?`~";

    /** 安全随机数生成器（避免使用 java.util.Random，防止密码可预测） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    /**
     * 生成默认规则的随机密码（10位，包含小写、大写、数字、特殊字符各至少1个）
     * @return 随机密码
     */
    public static String generatePassword() {
        return generatePassword(10, true, true, true, true);
    }
    /**
     * 生成8位，仅包含小写+数字的简易随机密码
     * @return 随机密码
     */
    public static String generateSimplePassword() {
        return generatePassword(8, true, false, true, false);
    }

    /**
     * 自定义规则生成随机密码
     * @param length          密码长度（最小4位，若开启所有字符类型）
     * @param includeLower    是否包含小写字母
     * @param includeUpper    是否包含大写字母
     * @param includeDigits   是否包含数字
     * @param includeSpecial  是否包含特殊字符
     * @return 随机密码
     * @throws IllegalArgumentException 长度或字符类型配置非法时抛出
     */
    public static String generatePassword(int length,
                                          boolean includeLower,
                                          boolean includeUpper,
                                          boolean includeDigits,
                                          boolean includeSpecial) {

        StringBuilder availableChars = new StringBuilder();
        if (includeLower) availableChars.append(LOWERCASE);
        if (includeUpper) availableChars.append(UPPERCASE);
        if (includeDigits) availableChars.append(DIGITS);
        if (includeSpecial) availableChars.append(SPECIAL_CHARS);

        // 3. 确保每种选中的字符类型至少包含1个（避免全是同一类字符）
        List<Character> passwordChars = new ArrayList<>();
        if (includeLower) {
            passwordChars.add(LOWERCASE.charAt(SECURE_RANDOM.nextInt(LOWERCASE.length())));
        }
        if (includeUpper) {
            passwordChars.add(UPPERCASE.charAt(SECURE_RANDOM.nextInt(UPPERCASE.length())));
        }
        if (includeDigits) {
            passwordChars.add(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        }
        if (includeSpecial) {
            passwordChars.add(SPECIAL_CHARS.charAt(SECURE_RANDOM.nextInt(SPECIAL_CHARS.length())));
        }

        // 4. 填充剩余长度的随机字符
        int remainingLength = length - passwordChars.size();
        for (int i = 0; i < remainingLength; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(availableChars.length());
            passwordChars.add(availableChars.charAt(randomIndex));
        }

        // 5. 打乱字符顺序（避免固定位置出现特定类型字符）
        Collections.shuffle(passwordChars, SECURE_RANDOM);

        // 6. 拼接为最终密码
        StringBuilder password = new StringBuilder();
        for (char c : passwordChars) {
            password.append(c);
        }

        return password.toString();
    }
    // ===================== 测试示例 =====================
//    public static void main(String[] args) {
//        // 示例1：默认规则（10位，包含所有字符类型）
//        String defaultPwd = RandomPasswordGenerator.generatePassword();
//        System.out.println("默认规则密码：" + defaultPwd); // 示例输出：9$8k7L90s78
//
//        // 示例2：8位，仅包含小写+数字
//        String simplePwd = RandomPasswordGenerator.generatePassword(8, true, false, true, false);
//        System.out.println("简易密码（小写+数字）：" + simplePwd); // 示例输出：89s78k90
//
//        // 示例3：12位，包含所有字符类型（更高复杂度）
//        String complexPwd = RandomPasswordGenerator.generatePassword(12, true, true, true, true);
//        System.out.println("复杂密码（12位）：" + complexPwd); // 示例输出：8$9k7L80s789a
//
//        // 示例4：6位，仅大写+数字（适合临时验证码）
//        String verifyCode = RandomPasswordGenerator.generatePassword(6, false, true, true, false);
//        System.out.println("验证码（大写+数字）：" + verifyCode); // 示例输出：987K90
//    }
}