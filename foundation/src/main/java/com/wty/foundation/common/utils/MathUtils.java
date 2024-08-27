package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;

public class MathUtils {
    private static final String TAG = "MathUtils";
    private static final float ACCURACY = 10E-7f;

    private MathUtils() {}

    /**
     * 比较两个浮点型是否相等，两个浮点型差的绝对值小于10E-7f相等。
     *
     * @param f1 第一个浮点型
     * @param f2 第二个浮点型
     * @return 如果相等返回true，否则返回false
     */
    public static boolean isEqual(float f1, float f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 比较两个双精度浮点型是否相等，两个浮点型差的绝对值小于10E-7f相等。
     *
     * @param f1 第一个双精度浮点型
     * @param f2 第二个双精度浮点型
     * @return 如果相等返回true，否则返回false
     */
    public static boolean isEqual(double f1, double f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 比较一个浮点型和一个双精度浮点型是否相等，差的绝对值小于10E-7f相等。
     *
     * @param f1 浮点型
     * @param f2 双精度浮点型
     * @return 如果相等返回true，否则返回false
     */
    public static boolean isEqual(double f1, float f2) {
        return Math.abs(f1 - f2) < ACCURACY;
    }

    /**
     * 字符串转int，失败返回默认值。
     *
     * @param str 字符串
     * @return int，默认值为0
     */
    public static int str2int(String str) {
        return str2int(str, 0);
    }

    /**
     * 字符串转int，失败返回默认值。
     *
     * @param str 字符串
     * @param defValue 默认值
     * @return int
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
     * 字符串转long，失败返回默认值。
     *
     * @param str 字符串
     * @return long，默认值为0
     */
    public static long str2long(String str) {
        return str2long(str, 0L);
    }

    /**
     * 字符串转long，失败返回默认值。
     *
     * @param str 字符串
     * @param defValue 默认值
     * @return long
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
     * 字符串转float，失败返回默认值。
     *
     * @param str 字符串
     * @return float，默认值为0
     */
    public static float str2float(String str) {
        return str2float(str, 0f);
    }

    /**
     * 字符串转float，失败返回默认值。
     *
     * @param str 字符串
     * @param defValue 默认值
     * @return float
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
     * 字符串转double，失败返回默认值。
     *
     * @param str 字符串
     * @return double，默认值为0
     */
    public static double str2double(String str) {
        return str2double(str, 0d);
    }

    /**
     * 字符串转double，失败返回默认值。
     *
     * @param str 字符串
     * @param defValue 默认值
     * @return double
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
     * 对单精度浮点数进行小数位截取。
     *
     * @param f 浮点数
     * @param accuracy 保留几位小数
     * @param mode 四舍五入模式
     * @return 截取后的结果
     */
    public static float floatScale(float f, int accuracy, RoundingMode mode) {
        try {
            BigDecimal decimal = new BigDecimal(Float.toString(f));
            decimal = decimal.setScale(accuracy, mode);
            return decimal.floatValue();
        } catch (Exception e) {
            Log.e(TAG, "floatScale", e);
            return 0f;
        }
    }

    /**
     * 对字符串表示的数字进行小数位截取。
     *
     * @param f 字符串形式的数字
     * @param accuracy 保留几位小数
     * @param mode 四舍五入模式
     * @return 截取后的结果
     */
    public static String doubleScale(String f, int accuracy, RoundingMode mode) {
        try {
            BigDecimal decimal = new BigDecimal(f);
            decimal = decimal.setScale(accuracy, mode);
            return decimal.toString();
        } catch (Exception e) {
            Log.e(TAG, "doubleScale", e);
            return "0";
        }
    }

    /**
     * 求两个整数的最大值。
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 最大值
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * 求两个整数的最小值。
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 最小值
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    /**
     * 求两个浮点数的最大值。
     *
     * @param a 第一个浮点数
     * @param b 第二个浮点数
     * @return 最大值
     */
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * 求两个浮点数的最小值。
     *
     * @param a 第一个浮点数
     * @param b 第二个浮点数
     * @return 最小值
     */
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /**
     * 求两个双精度浮点数的最大值。
     *
     * @param a 第一个双精度浮点数
     * @param b 第二个双精度浮点数
     * @return 最大值
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * 求两个双精度浮点数的最小值。
     *
     * @param a 第一个双精度浮点数
     * @param b 第二个双精度浮点数
     * @return 最小值
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    /**
     * 对整数进行四舍五入。
     *
     * @param value 整数值
     * @return 四舍五入后的结果
     */
    public static int roundToInt(double value) {
        return (int)Math.round(value);
    }

    /**
     * 对浮点数进行四舍五入。
     *
     * @param value 浮点数值
     * @return 四舍五入后的结果
     */
    public static float roundToFloat(double value) {
        return (float)Math.round(value);
    }

    /**
     * 对双精度浮点数进行四舍五入。
     *
     * @param value 双精度浮点数值
     * @return 四舍五入后的结果
     */
    public static double roundToDouble(double value) {
        return Math.round(value);
    }

    /**
     * 计算自然对数。
     *
     * @param x 数值
     * @return 自然对数
     */
    public static double log(double x) {
        return Math.log(x);
    }

    /**
     * 计算常用对数。
     *
     * @param x 数值
     * @return 常用对数
     */
    public static double log10(double x) {
        return Math.log10(x);
    }

    /**
     * 计算指数。
     *
     * @param x 数值
     * @return 指数
     */
    public static double exp(double x) {
        return Math.exp(x);
    }

    /**
     * 计算正弦值。
     *
     * @param x 角度值（弧度）
     * @return 正弦值
     */
    public static double sin(double x) {
        return Math.sin(x);
    }

    /**
     * 计算余弦值。
     *
     * @param x 角度值（弧度）
     * @return 余弦值
     */
    public static double cos(double x) {
        return Math.cos(x);
    }

    /**
     * 计算正切值。
     *
     * @param x 角度值（弧度）
     * @return 正切值
     */
    public static double tan(double x) {
        return Math.tan(x);
    }

    /**
     * 计算反正弦值。
     *
     * @param x 数值
     * @return 反正弦值
     */
    public static double asin(double x) {
        return Math.asin(x);
    }

    /**
     * 计算反余弦值。
     *
     * @param x 数值
     * @return 反余弦值
     */
    public static double acos(double x) {
        return Math.acos(x);
    }

    /**
     * 计算反正切值。
     *
     * @param x 数值
     * @return 反正切值
     */
    public static double atan(double x) {
        return Math.atan(x);
    }

    /**
     * 计算绝对值。
     *
     * @param x 数值
     * @return 绝对值
     */
    public static double abs(double x) {
        return Math.abs(x);
    }

    /**
     * 计算绝对值。
     *
     * @param x 数值
     * @return 绝对值
     */
    public static float abs(float x) {
        return Math.abs(x);
    }

    /**
     * 计算绝对值。
     *
     * @param x 数值
     * @return 绝对值
     */
    public static long abs(long x) {
        return Math.abs(x);
    }

    /**
     * 计算绝对值。
     *
     * @param x 数值
     * @return 绝对值
     */
    public static int abs(int x) {
        return Math.abs(x);
    }

    /**
     * 幂运算。
     *
     * @param base 底数
     * @param exponent 指数
     * @return 结果
     */
    public static double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    /**
     * 开方运算。
     *
     * @param x 数值
     * @return 开方结果
     */
    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    /**
     * 安全除法。
     *
     * @param dividend 被除数
     * @param divisor 除数
     * @param defaultValue 默认值
     * @return 结果
     */
    public static double safeDivide(double dividend, double divisor, double defaultValue) {
        if (divisor == 0) {
            return defaultValue;
        }
        return dividend / divisor;
    }

    /**
     * 随机整数。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机整数
     */
    public static int randomInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    /**
     * 随机双精度浮点数。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机双精度浮点数
     */
    public static double randomDouble(double min, double max) {
        Random rand = new Random();
        return rand.nextDouble() * (max - min) + min;
    }

    /**
     * 模运算。
     *
     * @param a 被模数
     * @param b 模数
     * @return 结果
     */
    public static double mod(double a, double b) {
        return a % b;
    }

    /**
     * 数组求和。
     *
     * @param numbers 数值数组
     * @return 和
     */
    public static double sum(double... numbers) {
        double total = 0;
        for (double number : numbers) {
            total += number;
        }
        return total;
    }

    /**
     * 数组求乘积。
     *
     * @param numbers 数值数组
     * @return 乘积
     */
    public static double product(double... numbers) {
        double result = 1;
        for (double number : numbers) {
            result *= number;
        }
        return result;
    }

    /**
     * 生成等差数列。
     *
     * @param start 起始值
     * @param end 结束值
     * @param step 步长
     * @return 等差数列
     */
    public static List<Double> arithmeticSequence(double start, double end, double step) {
        List<Double> sequence = new ArrayList<>();
        for (double i = start; i <= end; i += step) {
            sequence.add(i);
        }
        return sequence;
    }

    /**
     * 生成等比数列。
     *
     * @param start 起始值
     * @param end 结束值
     * @param ratio 比例因子
     * @return 等比数列
     */
    public static List<Double> geometricSequence(double start, double end, double ratio) {
        List<Double> sequence = new ArrayList<>();
        for (double i = start; i <= end; i *= ratio) {
            sequence.add(i);
        }
        return sequence;
    }
}
