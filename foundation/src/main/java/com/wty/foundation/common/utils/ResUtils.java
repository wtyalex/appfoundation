package com.wty.foundation.common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.wty.foundation.common.init.AppContext;


public class ResUtils {
    private static final String TAG = "ResUtils";

    // 私有构造函数防止外部实例化
    private ResUtils() {
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
     * 获取资源对象
     *
     * @return 当前应用程序的 Resources 对象
     */
    private static Resources getResources() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return context.getResources();
    }

    /**
     * 根据给定的字符串资源ID获取字符串
     *
     * @param id 字符串资源的ID
     * @return 对应的字符串资源内容；如果资源未找到或上下文为空，则返回空字符串
     */
    @Nullable
    public static String getString(@StringRes int id) {
        Resources resources = getResources();
        if (resources == null) {
            Log.e(TAG, "getString: resources is null");
            return "";
        }
        try {
            return resources.getString(id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return "";
        }
    }

    /**
     * 根据给定的颜色资源ID获取颜色值
     *
     * @param id 颜色资源的ID
     * @return 对应的颜色值；如果资源未找到或上下文为空，则返回透明颜色
     */
    @NonNull
    public static int getColor(@ColorRes int id) {
        Context context = getContext();
        if (context == null) {
            return Color.TRANSPARENT;
        }
        try {
            return ContextCompat.getColor(context, id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return Color.TRANSPARENT;
        }
    }

    /**
     * 根据给定的Drawable资源ID获取Drawable对象
     *
     * @param id Drawable资源的ID
     * @return 对应的Drawable对象；如果资源未找到或上下文为空，则返回null
     */
    @Nullable
    public static Drawable getDrawable(@DrawableRes int id) {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        try {
            return ContextCompat.getDrawable(context, id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return null;
        }
    }

    /**
     * 根据给定的尺寸资源ID获取像素大小
     *
     * @param id 尺寸资源的ID
     * @return 对应的像素大小；如果资源未找到或上下文为空，则返回0
     */
    public static int getDimensionPixelSize(@DimenRes int id) {
        Resources resources = getResources();
        if (resources == null) {
            Log.e(TAG, "getDimensionPixelSize: resources is null");
            return 0;
        }
        try {
            return resources.getDimensionPixelSize(id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return 0;
        }
    }

    /**
     * 根据给定的字符串数组资源ID获取字符串数组
     *
     * @param id 字符串数组资源的ID
     * @return 对应的字符串数组；如果资源未找到或上下文为空，则返回空数组
     */
    @NonNull
    public static String[] getStringArray(@ArrayRes int id) {
        Resources resources = getResources();
        if (resources == null) {
            Log.e(TAG, "getStringArray: resources is null");
            return new String[0];
        }
        try {
            return resources.getStringArray(id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return new String[0];
        }
    }

    /**
     * 根据给定的整型数组资源ID获取整型数组
     *
     * @param id 整型数组资源的ID
     * @return 对应的整型数组；如果资源未找到或上下文为空，则返回空数组
     */
    @NonNull
    public static int[] getIntArray(@ArrayRes int id) {
        Resources resources = getResources();
        if (resources == null) {
            Log.e(TAG, "getIntArray: resources is null");
            return new int[0];
        }
        try {
            return resources.getIntArray(id);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource ID not found: " + id, e);
            return new int[0];
        }
    }
}