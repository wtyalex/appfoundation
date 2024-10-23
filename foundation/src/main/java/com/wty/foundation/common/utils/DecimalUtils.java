package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import android.util.Log;

public class DecimalUtils {

    private static final String TAG = "DecimalUtils";
    private static final int DEFAULT_DECIMAL_PLACES = 2;

    // 私有构造函数，防止外部实例化
    private DecimalUtils() {}

    /**
     * 格式化一个数字字符串到指定小数位数
     *
     * @param num 要格式化的数字字符串
     * @param decimals 小数位数
     * @return 格式化后的字符串
     */
    public static String formatDecimal(String num, int decimals) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = new DecimalFormat("#,##0." + repeatChar('0', Math.max(0, decimals)));
            BigDecimal bd = new BigDecimal(num).setScale(decimals, RoundingMode.HALF_UP);
            return df.format(bd);
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatDecimal", e);
            return num;
        }
    }

    /**
     * 过滤输入字符串，只保留指定数量的小数位数
     *
     * @param input 输入字符串
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
     * 重复指定字符
     *
     * @param ch 要重复的字符
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
     * 格式化数字为货币形式
     *
     * @param num 数字字符串
     * @param currencyCode 货币代码
     * @return 货币格式化的字符串
     */
    public static String formatCurrency(String num, String currencyCode) {
        return formatCurrency(num, currencyCode, true);
    }

    /**
     * 格式化数字为货币形式，并指定符号位置
     *
     * @param num 数字字符串
     * @param currencyCode 货币代码
     * @param symbolBefore 是否将符号放在前面
     * @return 货币格式化的字符串
     */
    public static String formatCurrency(String num, String currencyCode, boolean symbolBefore) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = (DecimalFormat)NumberFormat.getCurrencyInstance();
            DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(currencyCode != null ? new Locale("en", currencyCode) : Locale.US);
            symbols.setCurrencySymbol(currencyCode != null ? Currency.getInstance(currencyCode).getSymbol() : "$");
            df.setDecimalFormatSymbols(symbols);
            df.setPositivePrefix(symbolBefore ? symbols.getCurrencySymbol() : "");
            df.setPositiveSuffix(symbolBefore ? "" : symbols.getCurrencySymbol());
            return df.format(new BigDecimal(num));
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatCurrency", e);
            return num;
        }
    }

    /**
     * 安全地解析一个字符串为 BigDecimal
     *
     * @param num 要解析的数字字符串
     * @return 解析后的 BigDecimal 对象
     */
    public static BigDecimal safeParseBigDecimal(String num) {
        if (StringUtils.isNullEmpty(num)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(num);
        } catch (NumberFormatException e) {
            Log.e(TAG, "safeParseBigDecimal ", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 格式化数字为科学计数法
     *
     * @param num 数字字符串
     * @param significantDigits 显著位数
     * @return 科学计数法格式化的字符串
     */
    public static String formatScientific(String num, int significantDigits) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = new DecimalFormat("0." + repeatChar('0', significantDigits - 1) + "E0");
            BigDecimal bd = new BigDecimal(num);
            return df.format(bd);
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatScientific", e);
            return num;
        }
    }

    /**
     * 格式化数字为百分比形式
     *
     * @param num 数字字符串
     * @param decimals 小数位数
     * @return 百分比格式化的字符串
     */
    public static String formatPercentage(String num, int decimals) {
        if (StringUtils.isNullEmpty(num)) {
            return "";
        }
        try {
            DecimalFormat df = new DecimalFormat("#,##0." + repeatChar('0', decimals) + "%");
            BigDecimal bd = new BigDecimal(num).setScale(decimals, RoundingMode.HALF_UP);
            return df.format(bd);
        } catch (NumberFormatException e) {
            Log.e(TAG, "formatPercentage", e);
            return num;
        }
    }

    /**
     * 安全地除以一个数，并返回结果（高精度的数值计算，如金融应用）
     *
     * 此方法执行高精度除法运算，并在除数为零时返回零 结果的小数位数可以通过参数 `scale` 指定，并采用四舍五入的方式进行舍入
     *
     * @param dividend 被除数
     * @param divisor 除数
     * @param scale 结果的小数位数
     * @return 计算结果，如果除数为零则返回零
     */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, scale, RoundingMode.HALF_UP);
    }

    /**
     * 四舍五入一个 BigDecimal 到指定小数位数
     *
     * @param value 要四舍五入的值
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
     * @param value 要向下取整的值
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
     * @param value 要向上取整的值
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