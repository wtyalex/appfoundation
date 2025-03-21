package com.wty.foundation.common.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private static final String TAG = "DateUtils";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String FRIENDLY_DATE_FORMAT = "dd/MM/yyyy HH:mm";

    // 私有构造函数防止外部实例化
    private DateUtils() {
    }

    /**
     * 将日期转换为字符串形式
     *
     * @param date    日期
     * @param pattern 日期格式
     * @param locale  地区设置
     * @return 日期字符串
     */
    public static String format(Date date, String pattern, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        return sdf.format(date);
    }

    /**
     * 使用默认地区设置格式化日期
     *
     * @param date    日期
     * @param pattern 日期格式
     * @return 日期字符串
     */
    public static String format(Date date, String pattern) {
        return format(date, pattern, Locale.getDefault());
    }

    /**
     * 解析日期字符串为日期对象.
     *
     * @param dateString 日期字符串
     * @param pattern    字符串的日期格式模板
     * @return 解析得到的日期对象
     * @throws ParseException 如果解析失败
     */
    public static Date parse(String dateString, String pattern) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.parse(dateString);
    }

    /**
     * 获取当前日期，格式为 "yyyy-MM-dd".
     *
     * @return 当前日期的字符串表示
     */
    public static String getCurrentDate() {
        return format(new Date(), DEFAULT_DATE_FORMAT);
    }

    /**
     * 获取当前日期时间，格式为 "yyyy-MM-dd HH:mm:ss".
     *
     * @return 当前日期时间的字符串表示
     */
    public static String getCurrentDateTime() {
        return format(new Date(), DEFAULT_DATETIME_FORMAT);
    }

    /**
     * 获取当前时间的指定字段的值
     *
     * @param field Calendar 字段（如 Calendar.YEAR, Calendar.MONTH 等）
     * @return 当前时间指定字段的值
     */
    public static int getCurrentField(int field) {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(field);
    }

    /**
     * 获取当前年份
     *
     * @return 当前年份
     */
    public static int getCurrentYear() {
        return getCurrentField(Calendar.YEAR);
    }

    /**
     * 获取当前月份（从 0 开始，即 0 表示 1 月）
     *
     * @return 当前月份
     */
    public static int getCurrentMonth() {
        return getCurrentField(Calendar.MONTH);
    }

    /**
     * 获取当前日
     *
     * @return 当前日
     */
    public static int getCurrentDay() {
        return getCurrentField(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取当前小时
     *
     * @return 当前小时
     */
    public static int getCurrentHour() {
        return getCurrentField(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取当前分钟
     *
     * @return 当前分钟
     */
    public static int getCurrentMinute() {
        return getCurrentField(Calendar.MINUTE);
    }

    /**
     * 获取当前秒
     *
     * @return 当前秒
     */
    public static int getCurrentSecond() {
        return getCurrentField(Calendar.SECOND);
    }

    /**
     * 获取特定日期的开始时间（毫秒）.
     *
     * @param date 特定日期
     * @return 该日期的开始时间（毫秒）
     */
    public static long getStartOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * 获取特定日期的结束时间（毫秒）.
     *
     * @param date 特定日期
     * @return 该日期的结束时间（毫秒）
     */
    public static long getEndOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    /**
     * 使用默认地区设置格式化日期为友好的字符串形式，并提供一种更易读的时间描述
     * <p>
     * 此方法根据给定日期与当前日期之间的差异，返回一个更易读的时间描述 时间描述根据以下规则生成： - 如果小于一分钟，则返回 "X 秒前" - 如果小于一小时，则返回 "X 分钟前" - 如果小于一天，则返回 "X 小时前" -
     * 如果小于一周，则返回 "X 天前" - 如果小于一个月，则返回 "X 周前" - 如果小于一年，则返回 "X 月前" - 如果超过一年，则返回 "dd/MM/yyyy HH:mm" 格式的日期时间
     *
     * @param date 要格式化的日期对象
     * @return 根据日期差异返回的时间描述字符串
     */
    public static String formatRelativeTime(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar past = Calendar.getInstance();
        past.setTime(date);
        long diff = (now.getTimeInMillis() - past.getTimeInMillis()) / 1000; // 获取时间差（秒）

        if (diff < 60) {
            return diff + " 秒前";
        } else if (diff < 3600) {
            return diff / 60 + " 分钟前";
        } else if (diff < 86400) {
            return diff / 3600 + " 小时前";
        } else if (diff < 86400 * 7) {
            return diff / 86400 + " 天前";
        } else if (diff < 86400 * 30) {
            return diff / (86400 * 7) + " 周前";
        } else if (diff < 86400 * 365) {
            return diff / (86400 * 30) + " 月前";
        } else {
            // 对于超过一年的时间，使用友好的日期格式显示
            SimpleDateFormat sdf = new SimpleDateFormat(FRIENDLY_DATE_FORMAT);
            return sdf.format(date);
        }
    }

    /**
     * 判断给定的年份是否是闰年
     *
     * @param year 需要判断的年份
     * @return 如果是闰年则返回 true，否则返回 false
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * 生成一个在指定范围内的随机日期
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 随机日期；如果开始日期晚于结束日期，则返回开始日期
     */
    public static Date generateRandomDate(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            Log.e(TAG, "输入的日期不能为空");
            return startDate != null ? startDate : endDate;
        }

        // 检查开始日期是否在结束日期之后
        if (startDate.after(endDate)) {
            Log.e(TAG, "开始日期必须在结束日期之前");
            return startDate;
        }

        long startMillis = startDate.getTime();
        long endMillis = endDate.getTime();
        long randomMillis = startMillis + (long) (Math.random() * (endMillis - startMillis));

        return new Date(randomMillis);
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有年份
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 年份列表
     */
    public static List<Integer> getYearsBetweenDates(Date startDate, Date endDate) {
        List<Integer> years = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        while (start.before(end)) {
            years.add(start.get(Calendar.YEAR));
            start.add(Calendar.YEAR, 1);
        }
        return years;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有月份
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 月份列表
     */
    public static List<String> getMonthsBetweenDates(Date startDate, Date endDate) {
        List<String> months = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        while (start.before(end)) {
            months.add(format(start.getTime(), "yyyy-MM"));
            start.add(Calendar.MONTH, 1);
        }
        return months;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有日期
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期列表
     */
    public static List<Date> getDaysBetweenDates(Date startDate, Date endDate) {
        List<Date> days = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        while (start.before(end)) {
            days.add(start.getTime());
            start.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有小时
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 小时列表
     */
    public static List<Date> getHoursBetweenDates(Date startDate, Date endDate) {
        List<Date> hours = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        while (start.before(end)) {
            hours.add(start.getTime());
            start.add(Calendar.HOUR_OF_DAY, 1);
        }
        return hours;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有分钟
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 分钟列表
     */
    public static List<Date> getMinutesBetweenDates(Date startDate, Date endDate) {
        List<Date> minutes = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        // 使用时间戳比较，确保循环正确执行
        while (start.getTimeInMillis() < end.getTimeInMillis()) {
            minutes.add(start.getTime());
            start.add(Calendar.MINUTE, 1);
        }
        return minutes;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有秒
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 秒列表
     */
    public static List<Date> getSecondsBetweenDates(Date startDate, Date endDate) {
        List<Date> seconds = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        // 使用时间戳比较，确保循环正确执行
        while (start.getTimeInMillis() < end.getTimeInMillis()) {
            seconds.add(start.getTime());
            start.add(Calendar.SECOND, 1);
        }
        return seconds;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间按指定时间间隔的时间戳
     *
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @param intervalTime 单位时间内的时间间隔
     * @param unit         时间间隔的单位（如：TimeUnit.MINUTES）
     * @return 时间戳列表
     */
    public static List<Long> getTimestampsBetweenDates(Date startDate, Date endDate, long intervalTime, TimeUnit unit) {
        List<Long> timestamps = new ArrayList<>();
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();

        // 计算时间间隔的毫秒数
        long intervalInMillis = unit.toMillis(intervalTime);

        // 循环直到达到结束时间
        for (long time = startTime; time <= endTime; time += intervalInMillis) {
            timestamps.add(time);
        }
        return timestamps;
    }

    /**
     * 返回一个列表，包含开始日期和结束日期之间的所有日期（字符串形式）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期字符串列表
     */
    public static List<String> getDatesBetweenDates(Date startDate, Date endDate) {
        List<String> dates = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        while (start.before(end)) {
            dates.add(format(start.getTime(), DEFAULT_DATE_FORMAT));
            start.add(Calendar.DATE, 1);
        }
        return dates;
    }

    /**
     * 获取指定月份的最大天数
     *
     * @param year  年份
     * @param month 月份
     * @return 该月的最大天数
     */
    public static int getMaxDaysInMonth(int year, int month) {
        Calendar calendar = new GregorianCalendar();
        calendar.setLenient(false); // 设置严格模式
        calendar.set(year, month - 1, 1); // 注意月份是从0开始的
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取指定日期的日
     *
     * @param date 日期
     * @return 日
     */
    public static int getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取指定日期的年份
     *
     * @param date 日期
     * @return 年份
     */
    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取指定日期的月份
     *
     * @param date 日期
     * @return 月份
     */
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1; // 注意月份是从0开始的
    }

    /**
     * 将日期转换为字符串形式（格式：yyyy-MM-dd）
     *
     * @param date   日期
     * @param locale 地区设置
     * @return 日期字符串
     */
    public static String dateToString(Date date, Locale locale) {
        return format(date, DEFAULT_DATE_FORMAT, locale);
    }

    /**
     * 将日期转换为字符串形式（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param date   日期
     * @param locale 地区设置
     * @return 日期字符串
     */
    public static String datetimeToString(Date date, Locale locale) {
        return format(date, DEFAULT_DATETIME_FORMAT, locale);
    }

    /**
     * 将字符串转换为日期对象（格式：yyyy-MM-dd）
     *
     * @param dateString 日期字符串
     * @param locale     地区设置
     * @return 日期对象
     * @throws ParseException 如果字符串无法被解析
     */
    public static Date stringToDate(String dateString, Locale locale) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT, locale);
        return sdf.parse(dateString);
    }

    /**
     * 将字符串转换为日期对象（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateString 时间字符串
     * @param locale     地区设置
     * @return 日期对象
     * @throws ParseException 如果字符串无法被解析
     */
    public static Date stringToTime(String dateString, Locale locale) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT, locale);
        return sdf.parse(dateString);
    }

    /**
     * 将时间戳转换为日期时间字符串.
     *
     * @param timestamp 时间戳
     * @param pattern   日期时间格式模板
     * @return 格式化后的日期时间字符串
     */
    public static String timestampToString(long timestamp, String pattern) {
        Date date = new Date(timestamp);
        return format(date, pattern);
    }

    /**
     * 将日期时间字符串转换为时间戳.
     *
     * @param dateString 日期时间字符串
     * @param pattern    字符串的日期时间格式模板
     * @return 转换后的时间戳
     * @throws ParseException 如果解析失败
     */
    public static long stringToTimestamp(String dateString, String pattern) throws ParseException {
        Date date = parse(dateString, pattern);
        return date.getTime();
    }

    /**
     * 计算两个日期之间的差值（天数）.
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 两个日期之间的天数差
     */
    public static long differenceBetweenDates(Date startDate, Date endDate) {
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        long diff = (end.getTimeInMillis() - start.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        return diff;
    }

    /**
     * 获取当前年份的前一年
     *
     * @return 前一年的年份字符串
     */
    public static String getPreviousYearString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        return Integer.toString(calendar.get(Calendar.YEAR));
    }

    /**
     * 获取当前年份的后一年
     *
     * @return 后一年的年份字符串
     */
    public static String getNextYearString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return Integer.toString(calendar.get(Calendar.YEAR));
    }

    /**
     * 获取当前月份的前一月
     *
     * @return 前一月的月份字符串
     */
    public static String getPreviousMonthString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return format(calendar.getTime(), "yyyy-MM");
    }

    /**
     * 获取当前月份的后一月
     *
     * @return 后一月的月份字符串
     */
    public static String getNextMonthString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        return format(calendar.getTime(), "yyyy-MM");
    }

    /**
     * 获取当前日的前一日
     *
     * @return 前一日的日期字符串
     */
    public static String getPreviousDayString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return format(calendar.getTime(), DEFAULT_DATE_FORMAT);
    }

    /**
     * 获取当前日的后一日
     *
     * @return 后一日的日期字符串
     */
    public static String getNextDayString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return format(calendar.getTime(), DEFAULT_DATE_FORMAT);
    }

    /**
     * 对指定日期进行加或减操作
     *
     * @param date   日期
     * @param amount 加减的数量
     * @param field  Calendar字段（如 Calendar.DAY_OF_MONTH）
     * @return 新的日期对象
     */
    public static Date addOrSubtract(Date date, int amount, int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    /**
     * 比较两个日期的大小
     *
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @return 如果 date1 在 date2 之前返回负数，如果相等返回零，否则返回正数
     */
    public static int compareDates(Date date1, Date date2) {
        return date1.compareTo(date2);
    }

    /**
     * 获取指定日期的前一年
     *
     * @param date 日期
     * @return 前一年的日期对象
     */
    public static Date getPreviousYear(Date date) {
        return addOrSubtract(date, -1, Calendar.YEAR);
    }

    /**
     * 获取指定日期的后一年
     *
     * @param date 日期
     * @return 后一年的日期对象
     */
    public static Date getNextYear(Date date) {
        return addOrSubtract(date, 1, Calendar.YEAR);
    }

    /**
     * 获取指定日期的前一个月
     *
     * @param date 日期
     * @return 前一个月的日期对象
     */
    public static Date getPreviousMonth(Date date) {
        return addOrSubtract(date, -1, Calendar.MONTH);
    }

    /**
     * 获取指定日期的后一个月
     *
     * @param date 日期
     * @return 后一个月的日期对象
     */
    public static Date getNextMonth(Date date) {
        return addOrSubtract(date, 1, Calendar.MONTH);
    }

    /**
     * 获取指定日期的前一天
     *
     * @param date 日期
     * @return 前一天的日期对象
     */
    public static Date getPreviousDay(Date date) {
        return addOrSubtract(date, -1, Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取指定日期的后一天
     *
     * @param date 日期
     * @return 后一天的日期对象
     */
    public static Date getNextDay(Date date) {
        return addOrSubtract(date, 1, Calendar.DAY_OF_MONTH);
    }

    /**
     * 检查两个日期是否在同一天
     *
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @return 如果在同一日则返回 true，否则返回 false
     */
    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 将日期从一个时区转换到另一个时区
     *
     * @param date         要转换的日期
     * @param fromTimeZone 日期当前所在的时区（如 "America/New_York"）
     * @param toTimeZone   转换目标的时区（如 "Asia/Shanghai"）
     * @return 转换后的日期；如果时区无效或发生其他错误，则返回原始日期
     */
    public static Date convertTimeZone(Date date, String fromTimeZone, String toTimeZone) {
        if (date == null || fromTimeZone == null || toTimeZone == null) {
            Log.e(TAG, "输入参数不能为空");
            return date;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT, Locale.getDefault());
        TimeZone fromTz = TimeZone.getTimeZone(fromTimeZone);
        TimeZone toTz = TimeZone.getTimeZone(toTimeZone);

        // 检查时区是否为GMT，如果是则认为是无效的时区ID（除了实际需要GMT的情况）
        if ("GMT".equals(fromTimeZone) && !"GMT".equals(toTimeZone) || "GMT".equals(toTimeZone) && !"GMT".equals(fromTimeZone)) {
            Log.e(TAG, "无效的时区: " + fromTimeZone + " 或 " + toTimeZone);
            return date;
        }

        try {
            sdf.setTimeZone(fromTz);
            String dateStr = sdf.format(date);
            sdf.setTimeZone(toTz);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "日期格式无效: " + date.toString(), e);
            return date;
        }
    }

    /**
     * 获取当前日期的时间戳（毫秒）
     *
     * @return 当前日期的时间戳
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 将时间戳转换为日期
     *
     * @param timestamp 时间戳
     * @return 日期对象
     */
    public static Date timestampToDate(long timestamp) {
        return new Date(timestamp);
    }

    /**
     * 计算两个日期之间的差值
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param unit      差值单位（如 Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY 等）
     * @return 差值
     * @throws IllegalArgumentException 如果单位无效
     */
    public static long getDifference(Date startDate, Date endDate, int unit) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(startDate);
        end.setTime(endDate);

        // 对于某些单位，如 DAY_OF_MONTH，不需要归零
        if (unit != Calendar.DAY_OF_MONTH && unit != Calendar.DAY_OF_YEAR) {
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);
        }

        return (long) (end.get(unit) - start.get(unit));
    }

    /**
     * 检查日期是否在某个范围内
     *
     * @param date      需要检查的日期
     * @param startDate 范围的开始日期
     * @param endDate   范围的结束日期
     * @return 如果在范围内返回 true，否则返回 false；如果输入参数无效，则记录错误并返回 false
     */
    public static boolean isWithinRange(Date date, Date startDate, Date endDate) {
        if (date == null || startDate == null || endDate == null) {
            Log.e(TAG, "输入的日期不能为空");
            return false;
        }
        if (startDate.after(endDate)) {
            Log.e(TAG, "开始日期 (" + startDate + ") 必须在结束日期 (" + endDate + ") 之前");
            return false;
        }
        return !date.before(startDate) && !date.after(endDate);
    }

    /**
     * 获取当前日期所在周的第一天和最后一天
     *
     * @return 一个包含两个元素的数组，其中第一个元素是周一的日期（周开始），第二个元素是周日的日期（周结束）
     */
    public static Date[] getFirstAndLastDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Monday
        Date weekStart = cal.getTime();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); // Sunday
        Date weekEnd = cal.getTime();
        return new Date[]{weekStart, weekEnd};
    }

    /**
     * 获取当前日期所在月的第一天和最后一天
     *
     * @return 一个包含两个元素的数组，其中第一个元素是当月第一天的日期（月开始），第二个元素是当月最后一天的日期（月结束）
     */
    public static Date[] getFirstAndLastDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1); // 设置为当月的第一天
        Date monthStart = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); // 设置为当月的最后一天
        Date monthEnd = cal.getTime();
        return new Date[]{monthStart, monthEnd};
    }

    /**
     * 修改日期对象的指定字段
     *
     * @param date  原始日期
     * @param field 要修改的字段（如 Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH 等）
     * @param value 新的值
     * @return 修改后的日期；如果字段无效或发生其他错误，则返回原始日期
     */
    public static Date setField(Date date, int field, int value) {
        if (date == null) {
            Log.e(TAG, "输入的日期不能为空");
            return date;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (field < 0 || field >= Calendar.FIELD_COUNT) {
            Log.e(TAG, "无效的日历字段: " + field + ". 必须在 0 和 " + (Calendar.FIELD_COUNT - 1) + " 之间");
            return date;
        }

        calendar.set(field, value);
        return calendar.getTime();
    }
}