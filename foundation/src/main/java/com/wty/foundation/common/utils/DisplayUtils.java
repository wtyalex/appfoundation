package com.wty.foundation.common.utils;

import com.wty.foundation.common.init.AppContext;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DisplayUtils {

    // 私有构造函数，禁止实例化
    private DisplayUtils() {}

    /**
     * 将 dp 值转换为 px 值，保证尺寸大小不变
     *
     * @param dpValue dp 值
     * @return 转换后的 px 值
     */
    public static int dpToPx(float dpValue) {
        final float scale = AppContext.getInstance().getContext().getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

    /**
     * 将 px 值转换为 dp 值，保证尺寸大小不变
     *
     * @param pxValue px 值
     * @return 转换后的 dp 值
     */
    public static int pxToDp(float pxValue) {
        final float scale = AppContext.getInstance().getContext().getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale + 0.5f);
    }

    /**
     * 将 sp 值转换为 px 值，保证文字大小不变
     *
     * @param spValue sp 值
     * @return 转换后的 px 值
     */
    public static int spToPx(float spValue) {
        final float fontScale = AppContext.getInstance().getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int)(spValue * fontScale + 0.5f);
    }

    /**
     * 将 px 值转换为 sp 值，保证文字大小不变
     *
     * @param pxValue px 值
     * @return 转换后的 sp 值
     */
    public static int pxToSp(float pxValue) {
        final float fontScale = AppContext.getInstance().getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue / fontScale + 0.5f);
    }

    /**
     * 获取屏幕宽度（以 dp 为单位）
     *
     * @return 屏幕宽度（dp）
     */
    public static int getScreenWidthInDp() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return (int)(displayMetrics.widthPixels / displayMetrics.density + 0.5f);
    }

    /**
     * 获取屏幕高度（以 dp 为单位）
     *
     * @return 屏幕高度（dp）
     */
    public static int getScreenHeightInDp() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return (int)(displayMetrics.heightPixels / displayMetrics.density + 0.5f);
    }

    /**
     * 获取屏幕宽度（以像素为单位）
     *
     * @return 屏幕宽度（像素）
     */
    public static int getScreenWidthInPx() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    /**
     * 获取屏幕高度（以像素为单位）
     *
     * @return 屏幕高度（像素）
     */
    public static int getScreenHeightInPx() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    /**
     * 获取屏幕密度
     *
     * @return 屏幕密度
     */
    public static float getScreenDensity() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

    /**
     * 获取屏幕密度 DPI
     *
     * @return 屏幕密度 DPI
     */
    public static int getScreenDensityDpi() {
        DisplayMetrics displayMetrics = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return displayMetrics.densityDpi;
    }

    /**
     * 获取屏幕分辨率（宽 x 高）
     *
     * @return 分辨率字符串（例如 "1920x1080"）
     */
    public static String getScreenResolution() {
        WindowManager windowManager =
            (WindowManager)AppContext.getInstance().getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x + "x" + size.y;
    }
}