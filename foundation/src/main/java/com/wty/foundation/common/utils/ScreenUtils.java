package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.wty.foundation.common.init.AppContext;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class ScreenUtils {

    private ScreenUtils() {}

    /**
     * 获取屏幕尺寸（英寸），比实际尺寸稍小。
     *
     * @return 屏幕尺寸
     */
    public static float calcScreenSize() {
        Context context = AppContext.getInstance().getContext();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return calcScreenSize(dm);
    }

    /**
     * 获取屏幕尺寸（英寸），比实际尺寸稍小。
     *
     * @param dm 显示度量
     * @return 屏幕尺寸
     */
    public static float calcScreenSize(DisplayMetrics dm) {
        double w = dm.widthPixels / dm.xdpi;
        double h = dm.heightPixels / dm.ydpi;
        BigDecimal bg = new BigDecimal(Math.sqrt(w * w + h * h)).setScale(2, RoundingMode.HALF_UP);
        return bg.floatValue();
    }

    /**
     * 获取屏幕大小（去除导航栏后）。
     *
     * @return 宽和高数组
     */
    public static int[] getScreenSize() {
        DisplayMetrics dm = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        int[] size = new int[2];
        size[0] = dm.widthPixels;
        size[1] = dm.heightPixels;
        return size;
    }

    /**
     * 获取状态栏高度。
     *
     * @return 状态栏高度
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
     * 获取导航栏高度。
     *
     * @return 导航栏高度
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
     * 设置状态栏属性。
     *
     * @param activity Activity
     * @param isVisibility 是否可见
     * @param isLayoutFull 是否全屏布局
     * @param color 背景色
     * @param isLight 是否浅色
     */
    public static void setStatusBar(@NonNull Activity activity, boolean isVisibility, boolean isLayoutFull, int color,
        boolean isLight) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(window, view);
        if (!isVisibility) {
            controllerCompat.hide(WindowInsetsCompat.Type.statusBars());
        } else {
            controllerCompat.show(WindowInsetsCompat.Type.statusBars());
            controllerCompat.setAppearanceLightStatusBars(isLight);
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
     * 隐藏状态栏。
     *
     * @param activity Activity
     */
    public static void hideStatusBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(window, view);
        controllerCompat.hide(WindowInsetsCompat.Type.statusBars());
    }

    /**
     * 显示状态栏。
     *
     * @param activity Activity
     */
    public static void showStatusBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View view = window.getDecorView();
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(window, view);
        controllerCompat.show(WindowInsetsCompat.Type.statusBars());
    }

    /**
     * 获取屏幕密度。
     *
     * @return 屏幕密度
     */
    public static float getScreenDensity() {
        DisplayMetrics dm = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return dm.density;
    }

    /**
     * 获取屏幕方向。
     *
     * @return 屏幕方向
     */
    public static int getScreenOrientation() {
        Activity activity = AppContext.getInstance().getContext() instanceof Activity
            ? (Activity)AppContext.getInstance().getContext() : null;
        if (activity != null) {
            return activity.getResources().getConfiguration().orientation;
        }
        return -1; // 返回-1表示无法确定
    }

    /**
     * 获取屏幕的物理尺寸（毫米）。
     *
     * @return 物理尺寸
     */
    public static Point getPhysicalScreenSize() {
        Display display =
            ((WindowManager)AppContext.getInstance().getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    /**
     * 获取屏幕DPI。
     *
     * @return DPI
     */
    public static int getScreenDPI() {
        DisplayMetrics dm = AppContext.getInstance().getContext().getResources().getDisplayMetrics();
        return dm.densityDpi;
    }

    /**
     * 判断屏幕是否为横屏。
     *
     * @return 是否横屏
     */
    public static boolean isLandscape() {
        return getScreenOrientation() == Configuration.ORIENTATION_LANDSCAPE;
    }
}
