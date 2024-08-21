package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.wty.foundation.common.init.AppContext;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class ScreenUtils {
    private ScreenUtils() {}

    /**
     * 获取屏幕的尺寸（英寸）会比真实尺寸小一丢丢
     * 
     * @return float
     */
    public static float calcScreenSize() {
        Context context = AppContext.getInstance().getContext();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return calcScreenSize(dm);
    }

    /**
     * 获取屏幕的尺寸（英寸）会比真实尺寸小一丢丢
     *
     * @return float
     */
    public static float calcScreenSize(DisplayMetrics dm) {
        double w = dm.widthPixels / dm.xdpi;
        double h = dm.heightPixels / dm.ydpi;
        BigDecimal bg = new BigDecimal(Math.sqrt(w * w + h * h)).setScale(2, RoundingMode.HALF_UP);
        return bg.floatValue();
    }

    /**
     * 获取去掉导航栏后的屏幕大小
     *
     * @return int[0]宽，int[1]高
     */
    public static int[] getScreenSize() {
        DisplayMetrics dm = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        int[] size = new int[2];
        size[0] = dm.widthPixels;
        size[1] = dm.heightPixels;
        return size;
    }

    /**
     * 获取状态栏高度，获取资源文件下status_bar_height的值
     *
     * @return int
     */
    public static int getStatusBarHeight() {
        Resources resources = AppContext.getInstance().getContext().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 获取导航栏高度，获取资源文件下navigation_bar_height的值
     *
     * @return int
     */
    public static int getNavigationBarHeight() {
        Resources resources = AppContext.getInstance().getContext().getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 设置状态栏
     *
     * @param activity Activity
     * @param isVisibility 是否显示状态栏
     * @param isLayoutFull 布局是否全局，显示状态栏下面，最好把状态栏背景色设置透明色
     * @param color 设置状态栏背景色
     * @param isLight 状态栏背景色是否是浅色，用于状态栏字体颜色
     */
    public static void setStatusBar(Activity activity, boolean isVisibility, boolean isLayoutFull, int color,
        boolean isLight) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = WindowCompat.getInsetsController(window, view);
        if (!isVisibility) {
            if (controllerCompat != null) {
                controllerCompat.hide(WindowInsetsCompat.Type.statusBars());
            }
        } else {
            if (controllerCompat != null) {
                controllerCompat.show(WindowInsetsCompat.Type.statusBars());
                controllerCompat.setAppearanceLightStatusBars(isLight);
            }
            if (isLayoutFull) {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            } else {
                view.setSystemUiVisibility(view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            if (color != 0) {
                window.setStatusBarColor(color);
            }
        }
    }

    /**
     * 隐藏状态栏
     *
     * @param activity Activity
     */
    public static void hideStatusBar(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = ViewCompat.getWindowInsetsController(view);
        if (controllerCompat == null) {
            return;
        }
        controllerCompat.hide(WindowInsetsCompat.Type.statusBars());
    }

    /**
     * 显示状态栏
     *
     * @param activity Activity
     */
    public static void showStatusBar(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = ViewCompat.getWindowInsetsController(view);
        if (controllerCompat == null) {
            return;
        }
        controllerCompat.show(WindowInsetsCompat.Type.statusBars());
    }
}
