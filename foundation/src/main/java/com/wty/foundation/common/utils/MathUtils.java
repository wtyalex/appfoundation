package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import android.util.Log;

public class MathUtils {
    private static final String TAG = "MathUtils";
    private static final float ACCURACY = 10E-7f;

    private MathUtils() {}

    /**
     * 比较两个浮点型是否相等，两个浮点型差的绝对值小于10E-7f相等
     *
     * @param f1 浮点型
     * @param f2 浮点型
     * @return boolean
     */
    public static boolean isEqual(float f1, float f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 比较两个浮点型是否相等，两个浮点型差的绝对值小于10E-7f相等
     *
     * @param f1 浮点型
     * @param f2 浮点型
     * @return boolean
     */
    public static boolean isEqual(double f1, double f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 比较两个浮点型是否相等，两个浮点型差的绝对值小于10E-7f相等
     *
     * @param f1 浮点型
     * @param f2 浮点型
     * @return boolean
     */
    public static boolean isEqual(double f1, float f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 字符串转int
     *
     * @param str String
     * @return int，失败返回0
     */
    public static int str2int(String str) {
        return str2int(str, 0);
    }

    /**
     * 字符串转int
     *
     * @param str String
     * @param defValue 默认值
     * @return int，失败返回defValue
     */
    public static int str2int(String str, int defValue) {
        try {
            BigDecimal decimal = new BigDecimal(str);
            return decimal.intValue();
        } catch (Exception e) {
            Log.e(TAG, "str2int", e);
            return defValue;
        }
    }

    /**
     * 字符串转long
     *
     * @param str String
     * @return long，失败返回0
     */
    public static long str2long(String str) {
        return str2long(str, 0l);
    }

    /**
     * 字符串转long
     *
     * @param str String
     * @param defValue 默认值
     * @return long，失败返回defValue
     */
    public static long str2long(String str, long defValue) {
        try {
            BigDecimal decimal = new BigDecimal(str);
            return decimal.longValue();
        } catch (Exception e) {
            Log.e(TAG, "str2long", e);
            return defValue;
        }
    }

    /**
     * 字符串转float
     *
     * @param str String
     * @return float，失败返回0
     */
    public static float str2float(String str) {
        return str2float(str, 0f);
    }

    /**
     * 字符串转int
     *
     * @param str String
     * @param defValue 默认值
     * @return float，失败返回defValue
     */
    public static float str2float(String str, float defValue) {
        try {
            BigDecimal decimal = new BigDecimal(str);
            return decimal.floatValue();
        } catch (Exception e) {
            Log.e(TAG, "str2float", e);
            return defValue;
        }
    }

    /**
     * 字符串转double
     *
     * @param str String
     * @return double，失败返回0
     */
    public static double str2double(String str) {
        return str2double(str, 0d);
    }

    /**
     * 字符串转int
     *
     * @param str String
     * @param defValue 默认值
     * @return double，失败返回defValue
     */
    public static double str2double(String str, double defValue) {
        try {
            BigDecimal decimal = new BigDecimal(str);
            return decimal.doubleValue();
        } catch (Exception e) {
            Log.e(TAG, "str2double", e);
            return defValue;
        }
    }

    /**
     * 单精度小数位截取
     * 
     * @param f
     * @param accuracy 保留几位小数
     * @return 结果
     */

    public static float floatScale(float f, int accuracy, RoundingMode mode) {
        try {
            BigDecimal decimal = new BigDecimal(f);
            decimal = decimal.setScale(accuracy, mode);
            return decimal.floatValue();
        } catch (Exception e) {
            Log.e(TAG, "floatScale", e);
            return 0f;
        }
    }

    /**
     * 单精度小数位截取
     * 
     * @param f
     * @param accuracy 保留几位小数
     * @return 结果
     */

    public static String doubleScale(String f, int accuracy, RoundingMode mode) {
        try {
            BigDecimal decimal = new BigDecimal(f);
            decimal = decimal.setScale(accuracy, mode);
            return decimal.toString();
        } catch (Exception e) {
            Log.e(TAG, "floatScale", e);
            return "0";
        }
    }
}
