package com.wty.foundation.common.utils;

import android.os.SystemClock;

public class TimeSync {
    private static long differ;
    private static boolean isSync;

    private TimeSync() {}

    public static void reset() {
        differ = 0;
        isSync = false;
    }

    public static void SyncTime(long time) {
        if (isSync) {
            return;
        }
        differ = time - SystemClock.elapsedRealtime();
        isSync = true;
    }

    public static long getTime() {
        if (isSync) {
            return SystemClock.elapsedRealtime() + differ;
        } else {
            return System.currentTimeMillis();
        }
    }
}
