package com.wty.foundation.common.utils;

import android.content.Context;
import android.util.Log;

import com.wty.foundation.common.init.AppContext;

/**
 * Author: 吴天宇
 * Date: 2024/8/27 16:56
 * Description: 屏幕尺寸转换工具类
 */
public class DisplayUtils {
    private static final String TAG = "DisplayUtils";

    // 私有构造函数，禁止实例化
    private DisplayUtils() {
    }

    /**
     * 获取全局上下文
     *
     * @return 应用程序的全局上下文对象
     */
    private static Context getContext() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
        }
        return context;
    }

    /**
     * 获取屏幕密度
     *
     * @return 屏幕密度，默认值为1.0
     */
    private static float getDensity() {
        Context context = getContext();
        if (context == null) {
            return 1.0f;
        }
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * 获取缩放后的屏幕密度
     *
     * @return 缩放后的屏幕密度，默认值为1.0
     */
    private static float getScaledDensity() {
        Context context = getContext();
        if (context == null) {
            return 1.0f;
        }
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * 将 dp 转换为 px
     *
     * @param dpValue 需转换的 dp 值
     * @return 对应的 px 值
     */
    public static int dpToPx(float dpValue) {
        float density = getDensity();
        return Math.round(dpValue * density);
    }

    /**
     * 将 px 转换为 dp
     *
     * @param pxValue 需转换的 px 值
     * @return 对应的 dp 值
     */
    public static int pxToDp(float pxValue) {
        float density = getDensity();
        return Math.round(pxValue / density);
    }

    /**
     * 将 sp 转换为 px
     *
     * @param spValue 需转换的 sp 值
     * @return 对应的 px 值
     */
    public static int spToPx(float spValue) {
        float scaledDensity = getScaledDensity();
        return Math.round(spValue * scaledDensity);
    }

    /**
     * 将 px 转换为 sp
     *
     * @param pxValue 需转换的 px 值
     * @return 对应的 sp 值
     */
    public static int pxToSp(float pxValue) {
        float scaledDensity = getScaledDensity();
        return Math.round(pxValue / scaledDensity);
    }
}