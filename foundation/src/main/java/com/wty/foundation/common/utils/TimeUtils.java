package com.wty.foundation.common.utils;

import android.util.Log;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 时间工具类，提供日期时间转换、解析、计算等功能
 */
public class TimeUtils {

    private static final String TAG = "TimeUtils";

    // 日期时间格式常量
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String HH_MM = "HH:mm";
    public static final String HH_MM_SS = "HH:mm:ss";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String GMT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final String YYYYMMDD_HHMMSS = "yyyyMMdd_HHmmss";

    // 自动解析时尝试的格式列表
    private static final String[] AUTO_PARSE_PATTERNS = {YYYY_MM_DD_HH_MM_SS, YYYY_MM_DD_HH_MM, YYYY_MM_DD, GMT_PATTERN, YYYYMMDD_HHMMSS};

    // 线程安全的SimpleDateFormat缓存，避免重复创建
    private static final ConcurrentMap<String, ThreadLocal<SimpleDateFormat>> SDF_CACHE = new ConcurrentHashMap<>();

    // 私有构造函数，防止实例化
    private TimeUtils() {

    }

    /**
     * 获取线程安全的SimpleDateFormat实例
     *
     * @param pattern 日期格式
     * @param locale  地区
     * @return 线程独立的SimpleDateFormat实例
     */
    private static SimpleDateFormat getSdf(String pattern, Locale locale) {
        String key = pattern + "_" + locale.toString();
        // 计算key的哈希值作为缓存键，减少内存占用
        key = Integer.toString(key.hashCode());

        // 如果缓存中没有，则创建新的ThreadLocal并放入缓存
        SDF_CACHE.putIfAbsent(key, new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(pattern, locale);
            }
        });

        return SDF_CACHE.get(key).get();
    }

    /**
     * 将时间对象转换为指定格式的字符串
     *
     * @param time   时间对象，可以是 Date、Calendar 或 Long 类型
     * @param format 日期时间格式，若为 null 则使用默认格式 yyyy-MM-dd HH:mm:ss
     * @return 格式化后的时间字符串，若出现异常则返回空字符串
     */
    public static String time2Str(@Nullable Object time, @Nullable String format) {
        String actualFormat = format != null ? format : YYYY_MM_DD_HH_MM_SS;
        SimpleDateFormat sdf = getSdf(actualFormat, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        try {
            Date date;
            if (time == null) {
                date = new Date();
            } else if (time instanceof Date) {
                date = (Date) time;
            } else if (time instanceof Calendar) {
                date = ((Calendar) time).getTime();
            } else if (time instanceof Long) {
                date = new Date((Long) time);
            } else {
                Log.w(TAG, "不支持的时间类型: " + time.getClass().getSimpleName());
                return "";
            }
            return sdf.format(date);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "无效的格式模板: " + actualFormat, e);
            return "";
        } catch (Exception e) {
            Log.e(TAG, "时间格式化失败", e);
            return "";
        }
    }

    /**
     * 将时间字符串解析为毫秒数
     *
     * @param time   时间字符串
     * @param format 日期时间格式，若为 null 则尝试自动解析
     * @return 解析后的毫秒数，若解析失败则返回 0
     */
    public static long str2Millis(@Nullable String time, @Nullable String format) {
        if (time == null || time.trim().isEmpty()) {
            Log.e(TAG, "输入的时间字符串为空");
            return 0L;
        }
        time = time.trim();

        try {
            if (format != null) {
                SimpleDateFormat sdf = getSdf(format, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getDefault());
                sdf.setLenient(false); // 严格解析模式
                return sdf.parse(time).getTime();
            } else {
                // 尝试自动解析常见格式
                for (String pattern : AUTO_PARSE_PATTERNS) {
                    try {
                        SimpleDateFormat sdf = getSdf(pattern, pattern.equals(GMT_PATTERN) ? Locale.US : Locale.getDefault());
                        sdf.setLenient(false);
                        sdf.setTimeZone(pattern.equals(GMT_PATTERN) ? TimeZone.getTimeZone("GMT") : TimeZone.getDefault());

                        Date date = sdf.parse(time);
                        if (date != null) {
                            return date.getTime();
                        }
                    } catch (ParseException ignored) {
                        // 解析失败则尝试下一种格式
                    }
                }
                Log.e(TAG, "自动解析失败，时间字符串: " + time);
                return 0L;
            }
        } catch (ParseException e) {
            Log.e(TAG, "解析失败，时间: " + time + " 格式: " + format, e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "无效的格式模板: " + format, e);
        }
        return 0L;
    }

    /**
     * 获取当前时间的毫秒数
     *
     * @return 当前时间的毫秒数
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 计算两个时间的差值
     *
     * @param startTime 开始时间的毫秒数
     * @param endTime   结束时间的毫秒数
     * @return 时间差值的绝对值（毫秒）
     */
    public static long calculateTimeDifference(long startTime, long endTime) {
        return Math.abs(endTime - startTime);
    }

    /**
     * 判断当前时间是否在指定时间范围内
     *
     * @param currentTime 当前时间的毫秒数
     * @param startTime   开始时间的毫秒数
     * @param endTime     结束时间的毫秒数
     * @return 若在范围内返回 true，否则返回 false
     */
    public static boolean isInTimeRange(long currentTime, long startTime, long endTime) {
        // 处理开始时间大于结束时间的情况（跨天）
        if (startTime > endTime) {
            return currentTime >= startTime || currentTime <= endTime;
        }
        return currentTime >= startTime && currentTime <= endTime;
    }

    /**
     * 设置日期的时分秒和毫秒并返回毫秒数
     *
     * @param date   日期
     * @param hour   小时（24小时制）
     * @param minute 分钟
     * @param second 秒
     * @param millis 毫秒
     * @return 设置后的时间的毫秒数
     */
    private static long setTimeFields(Date date, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal.getTimeInMillis();
    }

    /**
     * 获取指定日期当天的开始时间（00:00:00）的毫秒数
     *
     * @param date 指定日期
     * @return 当天开始时间的毫秒数，若 date 为 null 则返回 0
     */
    public static long getStartOfDay(@Nullable Date date) {
        if (date == null) {
            Log.e(TAG, "getStartOfDay: 日期为null");
            return 0L;
        }
        return setTimeFields(date, 0, 0, 0, 0);
    }

    /**
     * 获取指定日期当天的结束时间（23:59:59.999）的毫秒数
     *
     * @param date 指定日期
     * @return 当天结束时间的毫秒数，若 date 为 null 则返回 0
     */
    public static long getEndOfDay(@Nullable Date date) {
        if (date == null) {
            Log.e(TAG, "getEndOfDay: 日期为null");
            return 0L;
        }
        return setTimeFields(date, 23, 59, 59, 999);
    }

    /**
     * 获取指定日期所在月份的第一天的开始时间的毫秒数
     *
     * @param date 指定日期
     * @return 所在月份第一天开始时间的毫秒数，若 date 为 null 则返回 0
     */
    public static long getFirstDayOfMonth(@Nullable Date date) {
        if (date == null) {
            Log.e(TAG, "getFirstDayOfMonth: 日期为null");
            return 0L;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return getStartOfDay(cal.getTime());
    }

    /**
     * 获取指定日期所在月份的最后一天的结束时间的毫秒数
     *
     * @param date 指定日期
     * @return 所在月份最后一天结束时间的毫秒数，若 date 为 null 则返回 0
     */
    public static long getLastDayOfMonth(@Nullable Date date) {
        if (date == null) {
            Log.e(TAG, "getLastDayOfMonth: 日期为null");
            return 0L;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        return getEndOfDay(cal.getTime());
    }

    /**
     * 将 GMT 时间字符串转换为毫秒数（转换为系统默认时区）
     *
     * @param dateStr GMT 时间字符串，如 "Wed, 21 Oct 2015 07:28:00 GMT"
     * @return 转换后的毫秒数，若解析失败则返回 0
     */
    public static long gmt2Date(@Nullable String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            Log.e(TAG, "gmt2Date: 输入字符串为空");
            return 0L;
        }

        try {
            SimpleDateFormat sdf = getSdf(GMT_PATTERN, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            sdf.setLenient(false);

            Date date = sdf.parse(dateStr.trim());
            return date != null ? date.getTime() : 0L;
        } catch (ParseException e) {
            Log.e(TAG, "解析GMT时间失败: " + dateStr, e);
        }
        return 0L;
    }

    /**
     * 格式化时间戳为指定格式的字符串
     *
     * @param timestamp 时间戳（毫秒）
     * @param format    日期时间格式
     * @return 格式化后的时间字符串，若时间戳无效则返回空字符串
     */
    public static String formatTimestamp(long timestamp, @Nullable String format) {
        if (timestamp < 0) {
            Log.w(TAG, "无效的时间戳: " + timestamp);
            return "";
        }
        return time2Str(timestamp, format);
    }

    /**
     * 计算当前时间与指定时间戳的差值
     *
     * @param timestamp 指定时间戳（毫秒）
     * @return 时间差值的绝对值（毫秒）
     */
    public static long getTimeDifference(long timestamp) {
        return calculateTimeDifference(getCurrentTimeMillis(), timestamp);
    }

    /**
     * 判断两个时间戳是否为同一天
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return 若是同一天则返回true，否则返回false
     */
    public static boolean isSameDay(long time1, long time2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取指定时间戳几天后的时间戳
     *
     * @param timestamp 基准时间戳
     * @param days      天数（可为负数表示几天前）
     * @return 计算后的时间戳
     */
    public static long addDays(long timestamp, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTimeInMillis();
    }
}