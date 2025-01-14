package com.wty.foundation.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String HH_MM = "HH:mm";
    public static final String HH_MM_SS = "HH:mm:ss";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private TimeUtils() {
    }

    /**
     * 将时间转换为指定格式的字符串
     *
     * @param time   时间对象 (Date 或 Calendar) 或者 毫秒值
     * @param format 日期时间格式
     * @return 格式化后的字符串
     */
    public static String time2Str(Object time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        if (time instanceof Date) {
            return sdf.format((Date) time);
        } else if (time instanceof Calendar) {
            return sdf.format(((Calendar) time).getTime());
        } else if (time instanceof Long) {
            return sdf.format(new Date((Long) time));
        }
        throw new IllegalArgumentException("Unsupported time object type");
    }

    /**
     * 将字符串时间解析为毫秒值
     *
     * @param time   字符串时间
     * @param format 日期时间格式
     * @return 解析出的毫秒值；如果解析失败则返回 0
     */
    public static long str2Millis(String time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            return sdf.parse(time).getTime();
        } catch (ParseException e) {
            System.err.println("无法解析时间: " + time + " 格式: " + format);
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取当前时间的毫秒值
     *
     * @return 当前时间的毫秒值
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 计算两个时间戳之间的差值（毫秒）
     *
     * @param startTime 开始时间（毫秒）
     * @param endTime   结束时间（毫秒）
     * @return 时间差（毫秒）
     */
    public static long calculateTimeDifference(long startTime, long endTime) {
        return Math.abs(endTime - startTime);
    }

    /**
     * 判断给定的时间是否在指定的时间范围内
     *
     * @param currentTime 当前时间（毫秒）
     * @param startTime   开始时间（毫秒）
     * @param endTime     结束时间（毫秒）
     * @return 是否在时间范围内
     */
    public static boolean isInTimeRange(long currentTime, long startTime, long endTime) {
        return currentTime >= startTime && currentTime <= endTime;
    }

    /**
     * 设置给定日期的时间字段
     *
     * @param date        给定的日期
     * @param hourOfDay   小时
     * @param minute      分钟
     * @param second      秒
     * @param millisecond 毫秒
     * @return 设置后的时间戳（毫秒）
     */
    private static long setTimeFields(Date date, int hourOfDay, int minute, int second, int millisecond) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取给定日期的开始时间（毫秒），即当天00:00:00的时间戳
     *
     * @param date 给定的日期
     * @return 开始时间（毫秒）
     */
    public static long getStartOfDay(Date date) {
        return setTimeFields(date, 0, 0, 0, 0);
    }

    /**
     * 获取给定日期的结束时间（毫秒），即当天23:59:59的时间戳
     *
     * @param date 给定的日期
     * @return 结束时间（毫秒）
     */
    public static long getEndOfDay(Date date) {
        return setTimeFields(date, 23, 59, 59, 999);
    }

    /**
     * 获取给定日期所在月份的第一天（毫秒）
     *
     * @param date 给定的日期
     * @return 月份第一天的时间戳
     */
    public static long getFirstDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return setTimeFields(calendar.getTime(), 0, 0, 0, 0);
    }

    /**
     * 获取给定日期所在月份的最后一天（毫秒）
     *
     * @param date 给定的日期
     * @return 月份最后一天的时间戳
     */
    public static long getLastDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return setTimeFields(calendar.getTime(), 23, 59, 59, 999);
    }

    /**
     * 将格林威治时间（GMT）字符串转换为北京时间（GMT+8）的时间戳
     *
     * @param dateStr 格林威治时间字符串，格式为 "EEE, dd MMM yyyy HH:mm:ss z"，例如 "Mon, 01 Jan 2023 12:00:00 GMT"
     * @return 北京时间（GMT+8）的时间戳（毫秒），如果解析失败则返回 0
     */
    public static long gmt2Date(String dateStr) {
        // 使用 str2Millis 处理
        return str2Millis(dateStr, "EEE, dd MMM yyyy HH:mm:ss z");
    }

    /**
     * 格式化时间戳为指定格式的字符串
     *
     * @param timestamp 时间戳
     * @param format    日期时间格式
     * @return 格式化的字符串
     */
    public static String formatTimestamp(long timestamp, String format) {
        return time2Str(timestamp, format);
    }

    /**
     * 获取当前时间戳与指定时间戳之间的差值
     *
     * @param timestamp 指定时间戳
     * @return 差值（毫秒）
     */
    public static long getTimeDifference(long timestamp) {
        return calculateTimeDifference(System.currentTimeMillis(), timestamp);
    }
}
