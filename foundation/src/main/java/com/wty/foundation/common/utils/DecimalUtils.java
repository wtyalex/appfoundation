package com.wty.foundation.common.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Author: 吴天宇
 * Date: 2024/8/28 16:56
 * Description: 数值工具类，提供BigDecimal转换、数字格式化（货币/百分比/科学计数法）、数值计算及序列生成等功能
 */
public class DecimalUtils {

    private static final String TAG = "DecimalUtils";

    // 私有构造函数，防止外部实例化
    private DecimalUtils() {
    }

    /**
     * 安全地将对象转换为 BigDecimal
     *
     * @param num          要转换的对象
     * @param defaultValue 在转换失败时返回的默认值 (可选)
     * @return 转换后的 BigDecimal 对象 或 默认值
     */
    private static BigDecimal toBigDecimal(Object num, BigDecimal defaultValue) {
        try {
            if (num == null || "".equals(num.toString().trim())) {
                return defaultValue != null ? defaultValue : BigDecimal.ZERO;
            } else if (num instanceof String) {
                String numStr = ((String) num).trim();
                if ("".equals(numStr)) {
                    return defaultValue != null ? defaultValue : BigDecimal.ZERO;
                }
                return new BigDecimal(numStr);
            } else if (num instanceof Float || num instanceof Double) {
                return new BigDecimal(num.toString());
            } else if (num instanceof Integer || num instanceof Long) {
                return BigDecimal.valueOf(((Number) num).longValue());
            } else {
                Log.e(TAG, "Unsupported number type: " + num.getClass().getName());
                return defaultValue != null ? defaultValue : BigDecimal.ZERO;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error converting to BigDecimal", e);
            return defaultValue != null ? defaultValue : BigDecimal.ZERO;
        }
    }

    /**
     * 安全地将对象转换为 BigDecimal，默认默认值为 null
     *
     * @param num 要转换的对象
     * @return 转换后的 BigDecimal 对象或默认值
     */
    public static BigDecimal toBigDecimal(Object num) {
        return toBigDecimal(num, null);
    }

    /**
     * 安全地将字符串解析为 BigDecimal，默认值为零
     *
     * @param num 要解析的字符串
     * @return 解析后的 BigDecimal 对象或零
     */
    public static BigDecimal safeParseBigDecimal(String num) {
        return toBigDecimal(num, BigDecimal.ZERO);
    }

    /**
     * 格式化一个数字到指定小数位数
     *
     * @param num      要格式化的数字字符串或浮点数
     * @param accuracy 小数位数
     * @param mode     四舍五入模式
     * @return 格式化后的字符串
     */
    public static String formatNumber(Object num, int accuracy, RoundingMode mode) {
        try {
            BigDecimal decimal = toBigDecimal(num, BigDecimal.ZERO);
            DecimalFormat df = new DecimalFormat("#,##0." + repeatChar('0', Math.max(0, accuracy)));
            decimal = decimal.setScale(accuracy, mode);
            return df.format(decimal);
        } catch (Exception e) {
            Log.e(TAG, "formatNumber", e);
            return num != null ? num.toString() : "null";
        }
    }

    /**
     * 过滤输入字符串，只保留指定数量的小数位数
     *
     * @param input    输入字符串
     * @param decimals 允许的小数位数
     * @return 过滤后的字符串
     */
    public static String filterInput(String input, int decimals) {
        if (StringUtils.isNullEmpty(input)) {
            return "";
        }
        StringBuilder filtered = new StringBuilder();
        boolean decimalPointFound = false;
        int decimalCount = 0;

        for (char c : input.toCharArray()) {
            if (c == '.') {
                if (!decimalPointFound) {
                    decimalPointFound = true;
                    filtered.append(c);
                }
            } else if (Character.isDigit(c) || c == '-') {
                if (decimalPointFound && decimalCount < decimals) {
                    decimalCount++;
                }
                filtered.append(c);
            }
        }

        // 如果小数点后超过了指定的小数位数，截断字符串
        int lastDotIndex = filtered.lastIndexOf(".");
        if (lastDotIndex != -1 && filtered.length() - lastDotIndex - 1 > decimals) {
            filtered.setLength(lastDotIndex + decimals + 1);
        }

        return filtered.toString();
    }

    /**
     * 格式化数字为货币形式
     *
     * @param num          数字字符串
     * @param currencyCode 货币代码
     * @param symbolBefore 是否将符号放在前面
     * @return 货币格式化的字符串
     */
    public static String formatCurrency(String num, String currencyCode, boolean symbolBefore) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(currencyCode != null ? Locale.forLanguageTag(currencyCode) : Locale.US);
            symbols.setCurrencySymbol(currencyCode != null ? Currency.getInstance(currencyCode).getSymbol() : "$");
            df.setDecimalFormatSymbols(symbols);
            df.setPositivePrefix(symbolBefore ? symbols.getCurrencySymbol() : "");
            df.setPositiveSuffix(symbolBefore ? "" : symbols.getCurrencySymbol());
            return df.format(toBigDecimal(num, BigDecimal.ZERO));
        } catch (Exception e) {
            Log.e(TAG, "formatCurrency", e);
            return num;
        }
    }

    /**
     * 格式化数字为科学计数法
     *
     * @param num               数字字符串
     * @param significantDigits 显著位数
     * @return 科学计数法格式化的字符串
     */
    public static String formatScientific(String num, int significantDigits) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = new DecimalFormat("0." + repeatChar('0', significantDigits - 1) + "E0");
            return df.format(toBigDecimal(num, BigDecimal.ZERO));
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatScientific", e);
            return num;
        }
    }

    /**
     * 格式化数字为百分比形式
     *
     * @param num      数字字符串
     * @param decimals 小数位数
     * @return 百分比格式化的字符串
     */
    public static String formatPercentage(String num, int decimals) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = new DecimalFormat("#,##0." + repeatChar('0', decimals) + "%");
            BigDecimal bd = toBigDecimal(num, BigDecimal.ZERO).setScale(decimals, RoundingMode.HALF_UP);
            return df.format(bd);
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatPercentage", e);
            return num;
        }
    }

    /**
     * 安全地除以一个数，并返回结果（高精度的数值计算，如金融应用）
     *
     * @param dividend     被除数
     * @param divisor      除数
     * @param defaultValue 默认值（当除数为零时返回此值）
     * @return 除法的结果，如果除数为零则返回默认值
     */
    public static double safeDivide(double dividend, double divisor, double defaultValue) {
        return divisor == 0 ? defaultValue : dividend / divisor;
    }

    /**
     * 随机整数
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
     * 随机双精度浮点数
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
     * 数组求和
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
     * 数组求乘积
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
     * 生成等差数列
     *
     * @param start 起始值
     * @param end   结束值
     * @param step  步长
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
     * 生成等比数列
     *
     * @param start 起始值
     * @param end   结束值
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

    /**
     * 重复指定字符
     *
     * @param ch    要重复的字符
     * @param times 重复次数
     * @return 重复后的字符串
     */
    private static String repeatChar(char ch, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * 四舍五入一个 BigDecimal 到指定小数位数
     *
     * @param value  要四舍五入的值
     * @param places 小数位数
     * @return 四舍五入后的 BigDecimal
     */
    public static BigDecimal round(BigDecimal value, int places) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(places, RoundingMode.HALF_UP);
    }

    /**
     * 向下取整一个 BigDecimal 到指定小数位数
     *
     * @param value  要向下取整的值
     * @param places 小数位数
     * @return 向下取整后的 BigDecimal
     */
    public static BigDecimal floor(BigDecimal value, int places) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(places, RoundingMode.DOWN);
    }

    /**
     * 向上取整一个 BigDecimal 到指定小数位数
     *
     * @param value  要向上取整的值
     * @param places 小数位数
     * @return 向上取整后的 BigDecimal
     */
    public static BigDecimal ceil(BigDecimal value, int places) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(places, RoundingMode.UP);
    }
}