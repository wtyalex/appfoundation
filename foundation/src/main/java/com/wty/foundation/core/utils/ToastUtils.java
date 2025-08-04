package com.wty.foundation.core.utils;

import com.wty.foundation.common.init.AppContext;
import com.wty.foundation.common.utils.ResUtils;
import com.wty.foundation.common.utils.StringUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;

/**
 * @author wutianyu
 * @createTime 2023/1/18 10:00
 * @describe Toast工具类
 */
public class ToastUtils {
    private static final String TAG = "ToastUtils";
    private static Handler handler = new Handler(Looper.getMainLooper());

    private ToastUtils() {
    }

    /**
     * 短时间的Toast
     *
     * @param msg toast消息
     */
    public static void showShort(String msg) {
        show(msg, Toast.LENGTH_SHORT);
    }

    /**
     * 短时间的Toast
     *
     * @param msgId toast消息字符串资源Id
     */
    public static void showShort(@StringRes int msgId) {
        show(ResUtils.getString(msgId), Toast.LENGTH_SHORT);
    }

    /**
     * 长时间的Toast
     *
     * @param msg toast消息
     */
    public static void showLong(String msg) {
        show(msg, Toast.LENGTH_LONG);
    }

    /**
     * 长时间的Toast
     *
     * @param msgId toast消息字符串资源Id
     */
    public static void showLong(@StringRes int msgId) {
        show(ResUtils.getString(msgId), Toast.LENGTH_LONG);
    }

    private static void show(String msg, int time) {
        if (StringUtils.isNullEmpty(msg)) {
            return;
        }
        if (isMainThread()) {
            Context context = AppContext.getInstance().getContext();
            if (context == null) {
                Log.e(TAG, "Context cannot be null");
                return;
            }
            Toast.makeText(context, msg, time).show();
        } else {
            handler.post(() -> {
                Context context = AppContext.getInstance().getContext();
                if (context == null) {
                    Log.e(TAG, "Context cannot be null");
                    return;
                }
                Toast.makeText(context, msg, time).show();
            });
        }
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}