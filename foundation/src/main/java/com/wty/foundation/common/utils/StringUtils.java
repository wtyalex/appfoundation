package com.wty.foundation.common.utils;

public class StringUtils {
    private StringUtils() {}

    public static boolean isNull(String str) {
        return str == null;
    }

    public static boolean isNullEmpty(String str) {
        return isNull(str) || str.isEmpty();
    }

    public static boolean isNullEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isEqual(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equals(str2);
        }
    }

    public static String substringIndexOf(String str, String indexOf) {
        if (isNullEmpty(str)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.indexOf(indexOf) + 1);
        }
        return str;
    }

    public static String substringLastIndexOf(String str, String indexOf) {
        if (isNullEmpty(str)) {
            return str;
        }
        if (str.contains(indexOf)) {
            return str.substring(str.lastIndexOf(indexOf) + 1);
        }
        return str;
    }
}
