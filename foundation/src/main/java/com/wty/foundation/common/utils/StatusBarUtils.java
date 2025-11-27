package com.wty.foundation.common.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.wty.foundation.common.init.AppContext;

/**
 * @author wutianyu
 * @createTime 2025/1/10
 * @describe 状态栏工具类，提供沉浸式状态栏、状态栏颜色和图标颜色设置等功能
 */
public class StatusBarUtils {

    private static final String TAG = "StatusBarUtil";

    // 系统UI可见性标志的常量
    private static final int FLAG_TRANSLUCENT_STATUS = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
    private static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    private static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

    private StatusBarUtils() {
    }

    /**
     * 设置沉浸式状态栏
     *
     * @param activity Activity实例，用于获取窗口和装饰视图
     */
    public static void setImmersiveStatusBar(@NonNull Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                if (window == null) {
                    return;
                }
                View decorView = window.getDecorView();
                // 设置透明状态栏并使内容延伸到状态栏下方
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);

                // 确保布局能够延伸到状态栏下方，并使用浅色图标
                int uiOptions = SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(uiOptions);
                // 禁用 fitsSystemWindows 以允许内容延伸到状态栏下方
                ViewCompat.setFitsSystemWindows(decorView, false);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setTranslucentStatusFlag(activity.getWindow(), true);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置沉浸式状态栏失败", e);
        }
    }

    /**
     * 设置状态栏图标颜色（黑色/白色）
     *
     * @param activity  Activity实例，用于获取装饰视图
     * @param darkIcons 是否使用深色图标
     */
    public static void setStatusBarIconsColor(@NonNull Activity activity, boolean darkIcons) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setDarkIconsInternal(activity.getWindow().getDecorView(), darkIcons);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置状态栏图标颜色失败", e);
        }
    }

    /**
     * 获取主题中的状态栏颜色
     *
     * @param activity Activity实例，用于解析主题属性
     * @return 状态栏颜色（ARGB格式）
     */
    @ColorInt
    public static int getStatusBarColorFromTheme(@NonNull Activity activity) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.statusBarColor, typedValue, true);
        return typedValue.data;
    }

    /**
     * 设置状态栏颜色
     *
     * @param activity Activity实例，用于获取窗口
     * @param color    状态栏颜色（ARGB格式）
     */
    public static void setStatusBarColor(@NonNull Activity activity, @ColorInt int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setStatusBarInternal(activity, false, color, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置状态栏颜色失败", e);
        }
    }

    /**
     * 获取状态栏高度
     *
     * @return 状态栏高度（像素）
     */
    public static int getStatusBarHeight() {
        Resources resources = AppContext.getInstance().getContext().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    /**
     * 设置状态栏属性
     *
     * @param activity     Activity实例，用于获取窗口和装饰视图
     * @param isVisibility 是否显示状态栏
     * @param isLayoutFull 是否全屏布局
     * @param color        状态栏背景色（ARGB格式），如果为0则不改变
     * @param isLight      是否使用浅色图标
     */
    public static void setStatusBar(@NonNull Activity activity, boolean isVisibility, boolean isLayoutFull, int color, boolean isLight) {
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
            setDarkIconsInternal(view, isLight);
            if (isLayoutFull) {
                setFullScreenLayout(view);
            }
            if (color != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(color);
            }
        }
    }

    /**
     * 隐藏状态栏
     *
     * @param activity Activity实例，用于获取窗口和装饰视图
     */
    public static void hideStatusBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        new WindowInsetsControllerCompat(window, window.getDecorView()).hide(WindowInsetsCompat.Type.statusBars());
    }

    /**
     * 显示状态栏
     *
     * @param activity Activity实例，用于获取窗口和装饰视图
     */
    public static void showStatusBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        new WindowInsetsControllerCompat(window, window.getDecorView()).show(WindowInsetsCompat.Type.statusBars());
    }

    /**
     * 设置状态栏内部实现
     *
     * @param activity    Activity实例，用于获取窗口和装饰视图
     * @param translucent 是否设置透明状态栏
     * @param color       状态栏颜色（ARGB格式）
     * @param lightIcons  是否使用浅色图标
     */
    private static void setStatusBarInternal(@NonNull Activity activity, boolean translucent, @ColorInt int color, boolean lightIcons) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        if (translucent) {
            setTranslucentStatusFlag(window, true);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            setFullScreenLayout(window.getDecorView());
        } else {
            setTranslucentStatusFlag(window, false);
            window.setStatusBarColor(color);
        }
        setDarkIconsInternal(window.getDecorView(), lightIcons);
    }

    /**
     * 设置状态栏透明标志
     *
     * @param window Window实例，用于设置窗口标志
     * @param enable 是否启用透明状态栏
     */
    private static void setTranslucentStatusFlag(@NonNull Window window, boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (enable) {
                window.addFlags(FLAG_TRANSLUCENT_STATUS);
            } else {
                window.clearFlags(FLAG_TRANSLUCENT_STATUS);
            }
        }
    }

    /**
     * 设置状态栏图标为深色模式
     *
     * @param decorView 装饰视图，用于设置系统UI可见性标志
     * @param darkIcons 是否使用深色图标
     */
    private static void setDarkIconsInternal(@NonNull View decorView, boolean darkIcons) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int uiOptions = decorView.getSystemUiVisibility();
            decorView.setSystemUiVisibility(darkIcons ? (uiOptions & ~SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (uiOptions | SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
        }
    }

    /**
     * 设置全屏布局
     *
     * @param decorView 装饰视图，用于设置系统UI可见性标志
     */
    private static void setFullScreenLayout(@NonNull View decorView) {
        decorView.setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}