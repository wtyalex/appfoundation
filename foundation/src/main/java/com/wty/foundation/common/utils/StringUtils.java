package com.wty.foundation.common.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    // 私有构造函数，防止实例化
    private StringUtils() {}

    /**
     * 检查给定的字符串是否为null。
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null，则返回true；否则返回false
     */
    public static boolean isNull(String str) {
        return str == null;
    }

    /**
     * 检查给定的字符串是否为null或空字符串。
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null或长度为0，则返回true；否则返回false
     */
    public static boolean isNullEmpty(String str) {
        return isNull(str) || str.isEmpty();
    }

    /**
     * 检查给定的CharSequence对象是否为null或长度为0。
     *
     * @param str 要检查的CharSequence对象
     * @return 如果对象为null或长度为0，则返回true；否则返回false
     */
    public static boolean isNullEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 比较两个字符串是否相等。
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 如果两个字符串相等或都为null，则返回true；否则返回false
     */
    public static boolean isEqual(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equals(str2);
        }
    }

    /**
     * 获取从第一次出现指定子字符串之后的剩余部分。
     *
     * @param str 原始字符串
     * @param indexOf 子字符串
     * @return 如果找到子字符串，则返回子字符串之后的部分；否则返回原始字符串
     */
    public static String substringAfterFirst(String str, String indexOf) {
        if (isNullEmpty(str)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.indexOf(indexOf) + indexOf.length());
        }
        return str;
    }

    /**
     * 获取从最后一次出现指定子字符串之后的剩余部分。
     *
     * @param str 原始字符串
     * @param indexOf 子字符串
     * @return 如果找到子字符串，则返回子字符串之后的部分；否则返回原始字符串
     */
    public static String substringAfterLast(String str, String indexOf) {
        if (isNullEmpty(str)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.lastIndexOf(indexOf) + indexOf.length());
        }
        return str;
    }

    /**
     * 获取两个字符串的公共前缀。
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 公共前缀字符串
     */
    public static String commonPrefix(String str1, String str2) {
        int minLength = Math.min(str1.length(), str2.length());
        for (int i = 0; i < minLength; i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                return str1.substring(0, i);
            }
        }
        return str1.substring(0, minLength);
    }

    /**
     * 获取两个字符串的公共后缀。
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 公共后缀字符串
     */
    public static String commonSuffix(String str1, String str2) {
        int minLength = Math.min(str1.length(), str2.length());
        for (int i = 0; i < minLength; i++) {
            if (str1.charAt(str1.length() - 1 - i) != str2.charAt(str2.length() - 1 - i)) {
                return str1.substring(str1.length() - i);
            }
        }
        return str1.substring(str1.length() - minLength);
    }

    /**
     * 判断字符串是否只包含空白字符（如空格、制表符等）。
     *
     * @param str 要判断的字符串
     * @return 如果字符串只包含空白字符，则返回true；否则返回false
     */
    public static boolean isBlank(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将字符串转换为大写。
     *
     * @param str 要转换的字符串
     * @return 大写的字符串
     */
    public static String toUpperCase(String str) {
        if (isNullEmpty(str)) {
            return str;
        }
        return str.toUpperCase();
    }

    /**
     * 将字符串转换为小写。
     *
     * @param str 要转换的字符串
     * @return 小写的字符串
     */
    public static String toLowerCase(String str) {
        if (isNullEmpty(str)) {
            return str;
        }
        return str.toLowerCase();
    }

    /**
     * 去除字符串两端的空白字符。
     *
     * @param str 要处理的字符串
     * @return 去除空白字符后的字符串
     */
    public static String trim(String str) {
        if (isNullEmpty(str)) {
            return str;
        }
        return str.trim();
    }

    /**
     * 根据指定的分隔符将字符串分割成数组。
     *
     * @param str 原始字符串
     * @param delimiter 分隔符
     * @return 分割后的字符串数组
     */
    public static String[] split(String str, String delimiter) {
        if (isNullEmpty(str)) {
            return new String[] {str};
        }
        return str.split(delimiter);
    }

    /**
     * 将多个字符串或对象连接成一个字符串。
     *
     * @param separator 连接符
     * @param objects 对象数组
     * @return 连接后的字符串
     */
    public static String join(String separator, Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(objects[i].toString());
        }
        return sb.toString();
    }

    /**
     * 对字符串进行左填充或右填充以达到指定长度。
     *
     * @param str 原始字符串
     * @param length 目标长度
     * @param padStr 填充字符串
     * @param leftFill 是否左填充
     * @return 填充后的字符串
     */
    public static String pad(String str, int length, String padStr, boolean leftFill) {
        if (isNullEmpty(str)) {
            str = "";
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            if (leftFill) {
                sb.insert(0, padStr);
            } else {
                sb.append(padStr);
            }
        }
        return sb.toString().substring(0, length);
    }

    /**
     * 将字符串反转。
     *
     * @param str 要反转的字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (isNullEmpty(str)) {
            return str;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * 检查字符串是否匹配正则表达式。
     *
     * @param str 字符串
     * @param regex 正则表达式
     * @return 如果字符串匹配正则表达式，则返回true；否则返回false
     */
    public static boolean matchesRegex(String str, String regex) {
        if (isNullEmpty(str)) {
            return false;
        }
        return str.matches(regex);
    }

    /**
     * 对字符串进行URL编码。
     *
     * @param str 要编码的字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error encoding URL", e);
        }
    }

    /**
     * 对字符串进行URL解码。
     *
     * @param str 要解码的字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error decoding URL", e);
        }
    }

    /**
     * 将数字转换为字符串。
     *
     * @param number 数字
     * @return 转换后的字符串
     */
    public static String numberToString(Number number) {
        return number.toString();
    }

    /**
     * 将字符串转换为整数。
     *
     * @param str 字符串
     * @return 转换后的整数
     */
    public static int stringToInteger(String str) {
        return Integer.parseInt(str);
    }

    /**
     * 截取字符串到指定长度。
     *
     * @param str 字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (isNullEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * 创建一个字符串的重复副本。
     *
     * @param str 原始字符串
     * @param repeatCount 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int repeatCount) {
        if (isNullEmpty(str) || repeatCount <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeatCount; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 替换字符串中的特定字符或子字符串。
     *
     * @param str 原始字符串
     * @param oldSubstr 要被替换的子字符串
     * @param newSubstr 新的子字符串
     * @return 替换后的字符串
     */
    public static String replace(String str, String oldSubstr, String newSubstr) {
        if (isNullEmpty(str) || isNullEmpty(oldSubstr) || oldSubstr.isEmpty()) {
            return str;
        }
        return str.replace(oldSubstr, newSubstr);
    }

    /**
     * 检查字符串是否包含另一个字符串。
     *
     * @param str 主字符串
     * @param subStr 子字符串
     * @return 如果主字符串包含子字符串，则返回true；否则返回false
     */
    public static boolean contains(String str, String subStr) {
        if (isNullEmpty(str) || isNullEmpty(subStr)) {
            return false;
        }
        return str.contains(subStr);
    }

    /**
     * 统计字符串中特定字符或子字符串出现的次数。
     *
     * @param str 原始字符串
     * @param subStr 子字符串
     * @return 出现的次数
     */
    public static int countOccurrences(String str, String subStr) {
        if (isNullEmpty(str) || isNullEmpty(subStr)) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(subStr, idx)) != -1) {
            count++;
            idx += subStr.length();
        }
        return count;
    }

    /**
     * 格式化字符串输出，支持类似于 printf 的格式化。
     *
     * @param format 格式字符串
     * @param args 参数列表
     * @return 格式化后的字符串
     */
    public static String format(String format, Object... args) {
        return String.format(format, args);
    }

    /**
     * 基于多个分隔符拆分字符串。
     *
     * @param str 原始字符串
     * @param delimiters 分隔符数组
     * @return 拆分后的字符串数组
     */
    public static String[] splitByMultipleDelimiters(String str, String... delimiters) {
        if (isNullEmpty(str) || delimiters.length == 0) {
            return new String[] {str};
        }
        String regex = String.join("|", delimiters);
        return str.split(regex);
    }

    /**
     * 国际化和本地化支持，此处简化为直接返回字符串。
     *
     * @param key 键
     * @return 字符串资源
     */
    public static String getLocalizedString(String key) {
        // 在实际应用中，这里应该调用资源加载器获取本地化的字符串
        return key;
    }

    /**
     * 安全性检查，例如检查字符串是否可能包含 SQL 注入或 XSS 风险。
     *
     * @param str 字符串
     * @return 如果存在安全风险，则返回 true；否则返回 false
     */
    public static boolean hasSecurityRisk(String str) {
        // 这里只是一个示例实现，实际应用中应使用更复杂的安全检查机制
        String regex = "(;|--|\\bOR\\b)";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(str).find();
    }

    /**
     * 正则表达式相关操作，查找所有匹配项。
     *
     * @param str 字符串
     * @param regex 正则表达式
     * @return 匹配的字符串数组
     */
    public static String[] findMatches(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches.toArray(new String[0]);
    }

    /**
     * 正则表达式相关操作，替换所有匹配项。
     *
     * @param str 字符串
     * @param regex 正则表达式
     * @param replacement 替换字符串
     * @return 替换后的字符串
     */
    public static String replaceAllMatches(String str, String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll(replacement);
    }

    /**
     * 性能优化，对于大量数据处理，可能需要考虑字符串操作的性能优化。
     *
     * @param str 字符串
     * @return 性能优化后的字符串
     */
    public static String optimizePerformance(String str) {
        // 这里只是一个示例实现，实际应用中可能需要使用 StringBuilder 或其他优化技术
        return str;
    }

    /**
     * 自动检测字符串的编码，并进行相应的编码转换。
     *
     * @param bytes 字节流
     * @return 转换后的字符串
     */
    public static String detectAndConvertEncoding(byte[] bytes) {
        // 这里只是一个示例实现，实际应用中可能需要使用第三方库如 Apache Tika 来检测编码
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 生成字符串的哈希值，用于比较或存储等。
     *
     * @param str 字符串
     * @return 字符串的哈希值
     */
    public static String hash(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing string", e);
        }
    }
}
