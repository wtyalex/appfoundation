package com.wty.foundation.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.SystemClock;

public class TimeSync {

    private static long differ;
    private static boolean isSync;

    private TimeSync() {}

    /**
     * 重置时间同步状态。
     */
    public static void reset() {
        differ = 0;
        isSync = false;
    }

    /**
     * 同步时间到外部时间源。
     *
     * @param time 外部时间源的时间戳
     */
    public static void SyncTime(long time) {
        if (isSync) {
            return;
        }
        differ = time - SystemClock.elapsedRealtime();
        isSync = true;
    }

    /**
     * 获取经过同步后的时间戳。
     *
     * @return 同步后的时间戳
     */
    public static long getTime() {
        if (isSync) {
            return SystemClock.elapsedRealtime() + differ;
        } else {
            return System.currentTimeMillis();
        }
    }

    /**
     * 获取同步时间差。
     *
     * @return 时间差
     */
    public static long getDifference() {
        return differ;
    }

    /**
     * 检查时间是否已经同步。
     *
     * @return 是否已同步
     */
    public static boolean isSynced() {
        return isSync;
    }

    /**
     * 获取未经同步的系统时间戳。
     *
     * @return 未经同步的时间戳
     */
    public static long getSystemTime() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * 获取当前的时间戳，无论是否已经同步。
     *
     * @return 当前的时间戳
     */
    public static long getCurrentTime() {
        return isSync ? getTime() : System.currentTimeMillis();
    }

    /**
     * 获取当前时间的毫秒数。
     *
     * @return 当前时间的毫秒数
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 同步时间到UTC时间。
     *
     * @param utcTime UTC时间戳
     */
    public static void syncTimeToUtc(long utcTime) {
        if (isSync) {
            return;
        }
        differ = utcTime - System.currentTimeMillis();
        isSync = true;
    }

    /**
     * 获取同步时间对应的日期时间对象。
     *
     * @return 日期时间对象
     */
    public static Date getSyncedDate() {
        return new Date(getTime());
    }

    /**
     * 获取系统时间对应的日期时间对象。
     *
     * @return 日期时间对象
     */
    public static Date getSystemDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * 获取当前时间与同步时间的差值。
     *
     * @return 差值（毫秒）
     */
    public static long getTimeDifference() {
        return isSync ? System.currentTimeMillis() - getTime() : 0;
    }

    /**
     * 获取当前时间戳与指定时间戳之间的差值。
     *
     * @param timestamp 指定时间戳
     * @return 差值（毫秒）
     */
    public static long getTimeDifference(long timestamp) {
        return Math.abs(System.currentTimeMillis() - timestamp);
    }

    /**
     * 手动设置时间差。
     *
     * @param difference 时间差（毫秒）
     */
    public static void setDifference(long difference) {
        differ = difference;
    }

    /**
     * 格式化时间戳为指定格式的字符串。
     *
     * @param timestamp 时间戳
     * @param format 日期时间格式
     * @return 格式化的字符串
     */
    public static String formatTimestamp(long timestamp, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}