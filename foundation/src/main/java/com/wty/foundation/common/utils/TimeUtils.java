package com.wty.foundation.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtils {
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String hh_mm = "HH:mm";
    public static final String hh_mm_ss = "HH:mm:ss";
    public static final String YYYY_MM_DD_hh_mm = "yyyy-MM-dd HH:mm";

    private TimeUtils() {}

    public static String time2Str(long millis, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    public static String time2Str(Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    public static long str2Millis(String time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
