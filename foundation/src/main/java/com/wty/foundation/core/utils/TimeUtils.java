package com.wty.foundation.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2023/1/18 11:31
 * @describe
 */
public class TimeUtils {

    public static long gmt2Date(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        try {
            // 格林威治时间字符串转为Date格式 - 此Date是北京时间 GMT+8
            Date date = sdf.parse(dateStr);
            System.out.println("GMT+8 Date格式：" + date);
            // 格林威治时间字符串转为时间戳 - 此时间戳是北京时间 GMT+8
            long time = date.getTime();
            System.out.println("GMT+8 时间戳：" + time);
            return time;
        } catch (ParseException e) {
            Log.e("TimeUtils", Log.getStackTraceString(e));
        }
        return 0;
    }
}
