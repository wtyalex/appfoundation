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

    private ResUtils() {}

    public static String getString(@StringRes int id) {
        return RESOURCES.getString(id);
    }

    public static int getColor(@ColorRes int id) {
        return RESOURCES.getColor(id);
    }

    public static Drawable getDrawable(@DrawableRes int id) {
        return RESOURCES.getDrawable(id);
    }

    public static int getDimensionPixelSize(@DimenRes int id) {
        return RESOURCES.getDimensionPixelSize(id);
    }

    public static String[] getStringArray(@ArrayRes int id) {
        return RESOURCES.getStringArray(id);
    }
}
