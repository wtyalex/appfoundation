package com.wty.foundation.common.utils;

import com.wty.foundation.common.init.AppContext;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class ResUtils {
    private static final Resources RESOURCES = AppContext.getInstance().getContext().getResources();

    // 私有构造函数防止外部实例化
    private ResUtils() {}

    /**
     * 获取指定资源ID的字符串资源
     *
     * @param id 字符串资源的ID
     * @return 返回对应ID的字符串资源内容
     */
    public static String getString(@StringRes int id) {
        return RESOURCES.getString(id);
    }

    /**
     * 获取指定资源ID的颜色资源
     *
     * @param id 颜色资源的ID
     * @return 返回对应ID的颜色资源值
     */
    public static int getColor(@ColorRes int id) {
        return RESOURCES.getColor(id);
    }

    /**
     * 获取指定资源ID的Drawable资源
     *
     * @param id 图片资源的ID
     * @return 返回对应ID的Drawable对象
     */
    public static Drawable getDrawable(@DrawableRes int id) {
        return RESOURCES.getDrawable(id);
    }

    /**
     * 获取指定资源ID的尺寸资源，并将其转换为像素大小
     *
     * @param id 尺寸资源的ID
     * @return 返回对应ID的尺寸资源表示的像素大小
     */
    public static int getDimensionPixelSize(@DimenRes int id) {
        return RESOURCES.getDimensionPixelSize(id);
    }

    /**
     * 获取指定资源ID的字符串数组资源
     *
     * @param id 字符串数组资源的ID
     * @return 返回对应ID的字符串数组
     */
    public static String[] getStringArray(@ArrayRes int id) {
        return RESOURCES.getStringArray(id);
    }
}
