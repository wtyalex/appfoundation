package com.wty.foundation.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class TimeUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String HH_MM = "HH:mm";
    public static final String HH_MM_SS = "HH:mm:ss";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    private TimeUtils() {}

    /**
     * 将毫秒值转换为指定格式的字符串
     *
     * @param millis 毫秒值
     * @param format 日期时间格式
     * @return 格式化后的字符串
     */
    public static String time2Str(long millis, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * 将Calendar对象转换为指定格式的字符串
     *
     * @param calendar Calendar对象
     * @param format 日期时间格式
     * @return 格式化后的字符串
     */
    public static String time2Str(Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * 将字符串时间解析为毫秒值
     *
     * 此方法尝试将给定的字符串时间按照指定的日期时间格式解析为毫秒值 如果解析失败，则打印异常信息并返回 0
     *
     * @param time 字符串时间
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
     * 格式化当前时间为指定格式的字符串
     *
     * @param format 日期时间格式
     * @return 格式化后的字符串
     */
    public static String getCurrentTimeStr(String format) {
        return time2Str(System.currentTimeMillis(), format);
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
     * 获取当前时间的Calendar对象
     *
     * @return 当前时间的Calendar对象
     */
    public static Calendar getCurrentCalendar() {
        return Calendar.getInstance();
    }

    /**
     * 获取当前时间的Date对象
     *
     * @return 当前时间的Date对象
     */
    public static Date getCurrentDate() {
        return new Date();
    }

    /**
     * 计算两个时间戳之间的差值（毫秒）
     *
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     * @return 时间差（毫秒）
     */
    public static long calculateTimeDifference(long startTime, long endTime) {
        return Math.abs(endTime - startTime);
    }

    /**
     * 判断给定的时间是否在指定的时间范围内
     *
     * @param currentTime 当前时间（毫秒）
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     * @return 是否在时间范围内
     */
    public static boolean isInTimeRange(long currentTime, long startTime, long endTime) {
        return currentTime >= startTime && currentTime <= endTime;
    }

    /**
     * 获取给定日期的开始时间（毫秒），即当天00:00:00的时间戳
     *
     * @param date 给定的日期
     * @return 开始时间（毫秒）
     */
    public static long getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取给定日期的结束时间（毫秒），即当天23:59:59的时间戳
     *
     * @param date 给定的日期
     * @return 结束时间（毫秒）
     */
    public static long getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
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
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
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
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * 将格林威治时间（GMT）字符串转换为北京时间（GMT+8）的时间戳
     *
     * @param dateStr 格林威治时间字符串，格式为 "EEE, dd MMM yyyy HH:mm:ss z"，例如 "Mon, 01 Jan 2023 12:00:00 GMT"
     * @return 北京时间（GMT+8）的时间戳（毫秒），如果解析失败则返回 0
     */
    public static long gmt2Date(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        try {
            Date date = sdf.parse(dateStr);
            System.out.println("GMT+8 Date格式：" + date);

            long time = date.getTime();
            System.out.println("GMT+8 时间戳：" + time);
            return time;
        } catch (ParseException e) {
            Log.e("TimeUtils", Log.getStackTraceString(e));
        }
        return 0;
    }
}
