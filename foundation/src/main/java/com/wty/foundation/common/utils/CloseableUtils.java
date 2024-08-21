package com.wty.foundation.common.utils;

import java.io.Closeable;
import java.io.IOException;

import android.util.Log;

public class CloseableUtils {
    private static final String TAG = "CloseableUtils";

    private CloseableUtils() {}

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }
}
