package com.wty.foundation.common.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Function;

public class MathUtils {
    private static final String TAG = "MathUtils";
    private static final double ACCURACY = 1E-7;

    private MathUtils() {
    }

    /**
     * 比较两个浮点型是否相等
     *
     * @param f1 第一个浮点数
     * @param f2 第二个浮点数
     * @return 如果差的绝对值小于ACCURACY，则认为相等返回true；否则返回false
     */
    public static boolean isEqual(float f1, float f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 比较两个双精度浮点型是否相等
     *
     * @param d1 第一个双精度浮点数
     * @param d2 第二个双精度浮点数
     * @return 如果差的绝对值小于ACCURACY，则认为相等返回true；否则返回false
     */
    public static boolean isEqual(double d1, double d2) {
        return Math.abs(d1 - d2) < ACCURACY;
    }

    /**
     * 字符串转数值，失败返回默认值
     *
     * @param <T>       数值类型
     * @param str       要转换的字符串
     * @param defValue  默认值
     * @param converter 转换函数
     * @return 转换后的数值或默认值
     */
    private static <T> T convertString(String str, T defValue, Function<String, T> converter) {
        try {
            return converter.apply(str);
        } catch (Exception e) {
            Log.e(TAG, "转换错误: " + e.getMessage(), e);
            return defValue;
        }
    }

    /**
     * 字符串转int，失败返回默认值0
     *
     * @param str 字符串
     * @return int，默认值为0
     */
    public static int str2int(String str) {
        return str2int(str, 0);
    }

    /**
     * 字符串转int，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return int
     */
    public static int str2int(String str, int defValue) {
        return convertString(str, defValue, Integer::parseInt);
    }

    /**
     * 字符串转long，失败返回默认值0
     *
     * @param str 字符串
     * @return long，默认值为0
     */
    public static long str2long(String str) {
        return str2long(str, 0L);
    }

    /**
     * 字符串转long，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return long
     */
    public static long str2long(String str, long defValue) {
        return convertString(str, defValue, Long::parseLong);
    }

    /**
     * 字符串转float，失败返回默认值0
     *
     * @param str 字符串
     * @return float，默认值为0
     */
    public static float str2float(String str) {
        return str2float(str, 0f);
    }

    /**
     * 字符串转float，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return float
     */
    public static float str2float(String str, float defValue) {
        return convertString(str, defValue, Float::parseFloat);
    }

    /**
     * 字符串转double，失败返回默认值0
     *
     * @param str 字符串
     * @return double，默认值为0
     */
    public static double str2double(String str) {
        return str2double(str, 0d);
    }

    /**
     * 字符串转double，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return double
     */
    public static double str2double(String str, double defValue) {
        return convertString(str, defValue, Double::parseDouble);
    }

    /**
     * 字符串转BigInteger，失败返回默认值BigInteger.ZERO
     *
     * @param str 字符串
     * @return BigInteger，默认值为BigInteger.ZERO
     */
    public static BigInteger str2bigInteger(String str) {
        return str2bigInteger(str, BigInteger.ZERO);
    }

    /**
     * 字符串转BigInteger，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return BigInteger
     */
    public static BigInteger str2bigInteger(String str, BigInteger defValue) {
        return convertString(str, defValue, BigInteger::new);
    }

    /**
     * 字符串转BigDecimal，失败返回默认值BigDecimal.ZERO
     *
     * @param str 字符串
     * @return BigDecimal，默认值为BigDecimal.ZERO
     */
    public static BigDecimal str2bigDecimal(String str) {
        return str2bigDecimal(str, BigDecimal.ZERO);
    }

    /**
     * 字符串转BigDecimal，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return BigDecimal
     */
    public static BigDecimal str2bigDecimal(String str, BigDecimal defValue) {
        return convertString(str, defValue, BigDecimal::new);
    }

    /**
     * 字符串转boolean，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return boolean
     */
    public static boolean str2boolean(String str, boolean defValue) {
        return convertString(str, defValue, Boolean::parseBoolean);
    }

    /**
     * 字符串转boolean，失败返回默认值false
     *
     * @param str 字符串
     * @return boolean，默认值为false
     */
    public static boolean str2boolean(String str) {
        return str2boolean(str, false);
    }

    /**
     * 字符串转char，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return char
     */
    public static char str2char(String str, char defValue) {
        try {
            if (str != null && !str.isEmpty()) {
                return str.charAt(0);
            }
            return defValue;
        } catch (Exception e) {
            Log.e(TAG, "str2char", e);
            return defValue;
        }
    }

    /**
     * 字符串转char，失败返回默认值'\u0000'
     *
     * @param str 字符串
     * @return char，默认值为'\u0000'
     */
    public static char str2char(String str) {
        return str2char(str, '\u0000');
    }

    /**
     * 字符串转Character，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return Character
     */
    public static Character str2character(String str, Character defValue) {
        try {
            if (str != null && !str.isEmpty()) {
                return str.charAt(0);
            }
            return defValue;
        } catch (Exception e) {
            Log.e(TAG, "str2character", e);
            return defValue;
        }
    }

    /**
     * 字符串转Character，失败返回默认值null
     *
     * @param str 字符串
     * @return Character，默认值为null
     */
    public static Character str2character(String str) {
        return str2character(str, null);
    }

    /**
     * 字符串转UUID，失败返回默认值
     *
     * @param str      字符串
     * @param defValue 默认值
     * @return UUID
     */
    public static UUID str2uuid(String str, UUID defValue) {
        return convertString(str, defValue, UUID::fromString);
    }

    /**
     * 字符串转UUID，失败返回默认值null
     *
     * @param str 字符串
     * @return UUID，默认值为null
     */
    public static UUID str2uuid(String str) {
        return str2uuid(str, null);
    }
}
