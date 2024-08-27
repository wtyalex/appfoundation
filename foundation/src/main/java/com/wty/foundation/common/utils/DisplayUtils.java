package com.wty.foundation.common.utils;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtils {

    /**
     * 将 dp 值转换为 px 值，保证尺寸大小不变
     *
     * @param context 上下文
     * @param dpValue dp 值
     * @return 转换后的 px 值
     */
    public static int dpToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

    /**
     * 将 px 值转换为 dp 值，保证尺寸大小不变
     *
     * @param context 上下文
     * @param pxValue px 值
     * @return 转换后的 dp 值
     */
    public static int pxToDp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5f);
    }

    /**
     * 将 sp 值转换为 px 值，保证文字大小不变
     *
     * @param context 上下文
     * @param spValue sp 值
     * @return 转换后的 px 值
     */
    public static int spToPx(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(spValue * fontScale + 0.5f);
    }

    /**
     * 将 px 值转换为 sp 值，保证文字大小不变
     *
     * @param context 上下文
     * @param pxValue px 值
     * @return 转换后的 sp 值
     */
    public static int pxToSp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue / fontScale + 0.5f);
    }

    /**
     * 获取屏幕宽度（以 dp 为单位）
     *
     * @param context 上下文
     * @return 屏幕宽度（dp）
     */
    public static int getScreenWidthInDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)(displayMetrics.widthPixels / displayMetrics.density + 0.5f);
    }

    /**
     * 获取屏幕高度（以 dp 为单位）
     *
     * @param context 上下文
     * @return 屏幕高度（dp）
     */
    public static int getScreenHeightInDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)(displayMetrics.heightPixels / displayMetrics.density + 0.5f);
    }

    /**
     * 获取屏幕宽度（以像素为单位）
     *
     * @param context 上下文
     * @return 屏幕宽度（像素）
     */
    public static int getScreenWidthInPx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    /**
     * 获取屏幕高度（以像素为单位）
     *
     * @param context 上下文
     * @return 屏幕高度（像素）
     */
    public static int getScreenHeightInPx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    /**
     * 获取屏幕密度
     *
     * @param context 上下文
     * @return 屏幕密度
     */
    public static float getScreenDensity(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

    /**
     * 获取屏幕密度 DPI
     *
     * @param context 上下文
     * @return 屏幕密度 DPI
     */
    public static int getScreenDensityDpi(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.densityDpi;
    }

    /**
     * 获取屏幕分辨率（宽 x 高）
     *
     * @param context 上下文
     * @return 分辨率字符串（例如 "1920x1080"）
     */
    public static String getScreenResolution(Context context) {
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x + "x" + size.y;
    }
}