package com.wty.foundation.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

public class StringUtils {

    /**
     * 私有构造函数防止外部实例化
     */
    private StringUtils() {}

    /**
     * 检查给定字符串是否为null
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null返回true，否则返回false
     */
    public static boolean isNull(String str) {
        return str == null;
    }

    /**
     * 检查给定字符串是否为null或空
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null或空返回true，否则返回false
     */
    public static boolean isNullEmpty(String str) {
        return isNull(str) || str.isEmpty();
    }

    /**
     * 检查给定的CharSequence是否为null或空
     *
     * @param str 要检查的CharSequence
     * @return 如果CharSequence为null或空返回true，否则返回false
     */
    public static boolean isNullEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 检查给定字符串是否为null、空或空的JSON数组
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null、空或空的JSON数组返回true，否则返回false
     */
    public static boolean isEmptyJsonArray(String str) {
        if (isNullEmpty(str)) {
            return true;
        }
        try {
            JSONArray jsonArray = new JSONArray(str);
            return jsonArray.length() == 0;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 检查两个字符串是否相等
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 如果两个字符串相等返回true，否则返回false
     */
    public static boolean isEqual(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equals(str2);
        }
    }

    /**
     * 返回指定索引首次出现后的子字符串
     *
     * @param str 要处理的字符串
     * @param indexOf 要查找的索引
     * @return 索引之后的子字符串
     */
    public static String substringIndexOf(String str, String indexOf) {
        if (isNullEmpty(str) || isNullEmpty(indexOf)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.indexOf(indexOf) + indexOf.length());
        }
        return str;
    }

    /**
     * 返回指定索引最后一次出现后的子字符串
     *
     * @param str 要处理的字符串
     * @param indexOf 要查找的索引
     * @return 索引之后的子字符串
     */
    public static String substringLastIndexOf(String str, String indexOf) {
        if (isNullEmpty(str) || isNullEmpty(indexOf)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.lastIndexOf(indexOf) + indexOf.length());
        }
        return str;
    }

    /**
     * 检查给定字符串是否为null或空白（全是空格）
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null或空白返回true，否则返回false
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查给定的字符串是否可以转换为数字（整数或浮点数）
     *
     * @param str 要检查的字符串
     * @return 如果字符串可以转换为数字，则返回 true；否则返回 false
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查给定字符串是否不是空白
     *
     * @param str 要检查的字符串
     * @return 如果字符串不是空白返回true，否则返回false
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 去除字符串前后空白并返回，如果为null则返回空字符串
     *
     * @param str 要去除空白的字符串
     * @return 去除空白后的字符串或空字符串
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 去除字符串前后空白并返回，如果为空字符串则返回null
     *
     * @param str 要去除空白的字符串
     * @return 去除空白后的字符串或null
     */
    public static String trimToNull(String str) {
        final String trimmed = str == null ? null : str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 如果给定字符串为null或空，则返回默认字符串
     *
     * @param str 要检查的字符串
     * @param defaultStr 默认字符串
     * @return 给定的字符串或默认字符串
     */
    public static String defaultString(String str, String defaultStr) {
        return isNullEmpty(str) ? defaultStr : str;
    }

    /**
     * 在字符串左侧填充特定字符直到达到指定长度
     *
     * @param str 要填充的字符串
     * @param size 期望的长度
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            str = "";
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // 返回原字符串
        }
        if (pads > 8192) {
            throw new IllegalArgumentException("Padding length (" + pads + ") is too large");
        }
        return repeat(padChar, pads).concat(str);
    }

    /**
     * 重复一个字符多次
     *
     * @param ch 要重复的字符
     * @param repeat 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(char ch, int repeat) {
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    /**
     * 获取字符串中指定分隔符前的部分
     *
     * @param str 要处理的字符串
     * @param separator 分隔符
     * @return 分隔符前的部分
     */
    public static String getSubstringBefore(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return "";
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    /**
     * 获取字符串中指定分隔符后的一部分
     *
     * @param str 要处理的字符串
     * @param separator 分隔符
     * @return 分隔符后的一部分
     */
    public static String getSubstringAfter(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return str;
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    /**
     * 获取字符串中指定分隔符之间的部分
     *
     * @param str 要处理的字符串
     * @param startSeparator 开始分隔符
     * @param endSeparator 结束分隔符
     * @return 分隔符之间的部分
     */
    public static String getSubstringBetween(String str, String startSeparator, String endSeparator) {
        if (str == null || startSeparator == null || endSeparator == null) {
            return null;
        }
        int start = str.indexOf(startSeparator);
        if (start == -1) {
            return null;
        }
        int end = str.indexOf(endSeparator, start + startSeparator.length());
        if (end == -1) {
            return null;
        }
        return str.substring(start + startSeparator.length(), end);
    }

    /**
     * 将字符串转换为首字母大写
     *
     * @param str 要转换的字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * 将字符串转换为首字母小写
     *
     * @param str 要转换的字符串
     * @return 首字母小写的字符串
     */
    public static String lowercaseFirst(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * 反转字符串
     *
     * @param str 要反转的字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * 获取字符串中指定分隔符后的一部分，支持最后一个分隔符
     *
     * @param str 要处理的字符串
     * @param separator 分隔符
     * @return 分隔符后的一部分
     */
    public static String getSubstringAfterLast(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    /**
     * 替换字符串中的所有匹配项
     *
     * @param text 原始文本
     * @param searchString 要搜索的字符串
     * @param replacement 替换字符串
     * @return 替换后的字符串，如果原始文本或搜索字符串为null，则返回null
     */
    public static String replaceAll(String text, String searchString, String replacement) {
        if (searchString.isEmpty()) {
            return text;
        }
        int start = 0;
        int max = -1;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replacedLength = searchString.length();
        int increase = replacement.length() - replacedLength;
        increase = increase < 0 ? 0 : increase;
        increase *= (max < 0 ? text.length() / searchString.length() : max / searchString.length());
        StringBuilder sb = new StringBuilder(text.length() + increase);
        while (end != -1) {
            sb.append(text.substring(start, end)).append(replacement);
            start = end + replacedLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        sb.append(text.substring(start));
        return sb.toString();
    }

    /**
     * 判断字符串是否以指定的字符串结尾
     *
     * @param str 字符串
     * @param suffix 后缀
     * @return 如果字符串以指定的后缀结尾返回true，否则返回false
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        if (suffix.length() > str.length()) {
            return false;
        }
        return str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /**
     * 判断字符串是否以指定的字符串开头
     *
     * @param str 字符串
     * @param prefix 前缀
     * @return 如果字符串以指定的前缀开头返回true，否则返回false
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        if (prefix.length() > str.length()) {
            return false;
        }
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * 将字符串拆分成数组，忽略空字符串
     *
     * @param str 字符串
     * @param delimiter 分隔符
     * @return 字符串数组
     */
    public static String[] splitIgnoreEmpty(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return new String[] {str};
        }
        String[] parts = str.split(delimiter);
        List<String> nonEmptyParts = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                nonEmptyParts.add(part);
            }
        }
        return nonEmptyParts.toArray(new String[0]);
    }

    /**
     * 将字符串数组连接成一个字符串
     *
     * @param parts 字符串数组
     * @param delimiter 分隔符
     * @return 连接后的字符串
     */
    public static String join(String[] parts, String delimiter) {
        if (parts == null) {
            return null;
        }
        if (delimiter == null) {
            delimiter = "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            if (parts[i] != null) {
                builder.append(parts[i]);
            }
        }
        return builder.toString();
    }

    /**
     * 判断字符串是否包含指定的子字符串，忽略大小写
     *
     * @param str 主字符串
     * @param sub 子字符串
     * @return 如果主字符串包含子字符串返回true，否则返回false
     */
    public static boolean containsIgnoreCase(String str, String sub) {
        if (str == null || sub == null) {
            return false;
        }
        return str.toLowerCase().contains(sub.toLowerCase());
    }

    /**
     * 将字符串按照指定分隔符分割，保留空字符串
     *
     * @param str 字符串
     * @param delimiter 分隔符
     * @return 字符串数组
     */
    public static String[] splitPreserveEmpty(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return new String[] {str};
        }
        return str.split(Pattern.quote(delimiter), -1);
    }

    /**
     * 将字符串按照指定分隔符分割，并去除每个元素的空白
     *
     * @param str 字符串
     * @param delimiter 分隔符
     * @return 字符串数组
     */
    public static String[] splitTrim(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return new String[] {str};
        }
        String[] parts = str.split(delimiter);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    /**
     * 将字符串按照指定分隔符分割，并去除每个元素的空白，同时忽略空字符串
     *
     * @param str 字符串
     * @param delimiter 分隔符
     * @return 字符串数组
     */
    public static String[] splitTrimIgnoreEmpty(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return new String[] {str};
        }
        String[] parts = str.split(delimiter);
        List<String> trimmedParts = new ArrayList<>();
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                trimmedParts.add(trimmedPart);
            }
        }
        return trimmedParts.toArray(new String[0]);
    }

    /**
     * 删除字符串前导和尾随的指定字符
     *
     * @param str 字符串
     * @param trimChars 要删除的字符集
     * @return 去除了指定字符的字符串
     */
    public static String trim(String str, String trimChars) {
        if (str == null || trimChars == null) {
            return str;
        }
        int start = 0;
        int end = str.length();
        while (start < end && trimChars.indexOf(str.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && trimChars.indexOf(str.charAt(end - 1)) >= 0) {
            end--;
        }
        return str.substring(start, end);
    }

    /**
     * 删除字符串前导的指定字符
     *
     * @param str 字符串
     * @param trimChars 要删除的字符集
     * @return 去除了前导指定字符的字符串
     */
    public static String trimLeadingChars(String str, String trimChars) {
        if (str == null || trimChars == null) {
            return str;
        }
        int start = 0;
        while (start < str.length() && trimChars.indexOf(str.charAt(start)) >= 0) {
            start++;
        }
        return str.substring(start);
    }

    /**
     * 删除字符串尾随的指定字符
     *
     * @param str 字符串
     * @param trimChars 要删除的字符集
     * @return 去除了尾随指定字符的字符串
     */
    public static String trimTrailingChars(String str, String trimChars) {
        if (str == null || trimChars == null) {
            return str;
        }
        int end = str.length();
        while (end > 0 && trimChars.indexOf(str.charAt(end - 1)) >= 0) {
            end--;
        }
        return str.substring(0, end);
    }

    /**
     * 从字符串中移除指定的字符
     *
     * @param str 字符串
     * @param removeChar 要移除的字符
     * @return 移除指定字符后的字符串
     */
    public static String removeChar(String str, char removeChar) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c != removeChar) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 从字符串中移除指定的字符集合
     *
     * @param str 字符串
     * @param removeChars 要移除的字符集合
     * @return 移除指定字符集合后的字符串
     */
    public static String removeChars(String str, String removeChars) {
        if (str == null || removeChars == null) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (removeChars.indexOf(c) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将字符串中的指定字符替换为另一个字符
     *
     * @param str 字符串
     * @param oldChar 要替换的字符
     * @param newChar 新字符
     * @return 替换后的字符串
     */
    public static String replaceChar(String str, char oldChar, char newChar) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(c == oldChar ? newChar : c);
        }
        return sb.toString();
    }

    /**
     * 将字符串中的指定字符集合替换为另一个字符
     *
     * @param str 字符串
     * @param oldChars 要替换的字符集合
     * @param newChar 新字符
     * @return 替换后的字符串
     */
    public static String replaceChars(String str, String oldChars, char newChar) {
        if (str == null || oldChars == null) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (oldChars.indexOf(c) >= 0) {
                sb.append(newChar);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
