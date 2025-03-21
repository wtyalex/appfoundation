package com.wty.foundation.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeUtils {

    private static final Logger Log = Logger.getLogger(TimeUtils.class.getName());
    private static final String TAG = "TimeUtils";

    // 日期格式：年-月-日
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    // 时间格式：时:分
    public static final String HH_MM = "HH:mm";
    // 时间格式：时:分:秒
    public static final String HH_MM_SS = "HH:mm:ss";
    // 日期时间格式：年-月-日 时:分
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    // 日期时间格式：年-月-日 时:分:秒
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    // 私有构造函数，防止实例化
    private TimeUtils() {
    }

    /**
     * 将时间对象转换为指定格式的字符串
     *
     * @param time   时间对象，可以是 Date、Calendar 或 Long 类型
     * @param format 日期时间格式，若为 null 则使用默认格式 yyyy-MM-dd HH:mm:ss
     * @return 格式化后的时间字符串，若出现异常则返回空字符串
     */
    public static String time2Str(Object time, String format) {
        String actualFormat = format != null ? format : YYYY_MM_DD_HH_MM_SS;
        SimpleDateFormat sdf = new SimpleDateFormat(actualFormat, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        try {
            if (time == null) {
                return sdf.format(new Date());
            } else if (time instanceof Date) {
                return sdf.format((Date) time);
            } else if (time instanceof Calendar) {
                return sdf.format(((Calendar) time).getTime());
            } else if (time instanceof Long) {
                return sdf.format(new Date((Long) time));
            } else {
                Log.log(Level.WARNING, "Unsupported time type: " + time.getClass().getSimpleName());
                return "";
            }
        } catch (IllegalArgumentException e) {
            Log.log(Level.SEVERE, "Invalid format pattern: " + actualFormat, e);
            return "";
        } catch (Exception e) {
            Log.log(Level.SEVERE, "Format time error", e);
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
    public static long str2Millis(String time, String format) {
        if (time == null || time.isEmpty()) {
            Log.log(Level.SEVERE, "Input time string is empty");
            return 0L;
        }

        SimpleDateFormat sdf;
        try {
            if (format != null) {
                sdf = new SimpleDateFormat(format, Locale.getDefault());
            } else { // 尝试常见格式自动解析
                String[] patterns = {YYYY_MM_DD_HH_MM_SS, YYYY_MM_DD_HH_MM, YYYY_MM_DD, "EEE, dd MMM yyyy HH:mm:ss z", "yyyyMMdd_HHmmss"};

                for (String pattern : patterns) {
                    try {
                        sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                        sdf.setLenient(false);
                        Date date = sdf.parse(time);
                        return date.getTime();
                    } catch (ParseException ignored) {
                    }
                }
                Log.log(Level.SEVERE, "Auto parse failed for: " + time);
                return 0L;
            }

            sdf.setLenient(false);
            sdf.setTimeZone(TimeZone.getDefault());
            return sdf.parse(time).getTime();
        } catch (ParseException e) {
            Log.log(Level.SEVERE, "Parse failed. Time: " + time + " Format: " + format, e);
        } catch (IllegalArgumentException e) {
            Log.log(Level.SEVERE, "Invalid format pattern: " + format, e);
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
     * @return 时间差值的绝对值
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
        return currentTime >= startTime && currentTime <= endTime;
    }

    /**
     * 设置日期的时分秒和毫秒并返回毫秒数
     *
     * @param date   日期
     * @param hour   小时
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
    public static long getStartOfDay(Date date) {
        if (date == null) {
            Log.log(Level.SEVERE, "getStartOfDay: date is null");
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
    public static long getEndOfDay(Date date) {
        if (date == null) {
            Log.log(Level.SEVERE, "getEndOfDay: date is null");
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
    public static long getFirstDayOfMonth(Date date) {
        if (date == null) {
            Log.log(Level.SEVERE, "getFirstDayOfMonth: date is null");
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
    public static long getLastDayOfMonth(Date date) {
        if (date == null) {
            Log.log(Level.SEVERE, "getLastDayOfMonth: date is null");
            return 0L;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        return getEndOfDay(cal.getTime());
    }

    /**
     * 将 GMT 时间字符串转换为毫秒数
     *
     * @param dateStr GMT 时间字符串
     * @return 转换后的毫秒数，若解析失败则返回 0
     */
    public static long gmt2Date(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            Log.log(Level.SEVERE, "gmt2Date: input is empty");
            return 0L;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date date = sdf.parse(dateStr);
            // 转换为北京时间（时区偏移由系统处理）
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            cal.setTime(date);
            return cal.getTimeInMillis();
        } catch (ParseException e) {
            Log.log(Level.SEVERE, "Parse GMT time failed: " + dateStr, e);
        }
        return 0L;
    }

    /**
     * 格式化时间戳为指定格式的字符串
     *
     * @param timestamp 时间戳
     * @param format    日期时间格式
     * @return 格式化后的时间字符串，若时间戳无效则返回空字符串
     */
    public static String formatTimestamp(long timestamp, String format) {
        if (timestamp < 0) {
            Log.log(Level.WARNING, "Invalid timestamp: " + timestamp);
            return "";
        }
        return time2Str(timestamp, format);
    }

    /**
     * 计算当前时间与指定时间戳的差值
     *
     * @param timestamp 指定时间戳
     * @return 时间差值的绝对值
     */
    public static long getTimeDifference(long timestamp) {
        return calculateTimeDifference(getCurrentTimeMillis(), timestamp);
    }
}