package com.wty.foundation.common.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.wty.foundation.common.init.AppContext;


public class NavigationBarUtils {

    // 私有构造函数防止外部实例化
    private NavigationBarUtils() {
    }

    /**
     * 获取导航栏高度
     *
     * @return 导航栏高度（像素）
     */
    public static int getNavigationBarHeight() {
        Resources resources = AppContext.getInstance().getContext().getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    /**
     * 判断是否存在导航栏
     *
     * @param activity Activity实例
     * @return 是否存在导航栏
     */
    public static boolean hasNavigationBar(@NonNull Activity activity) {
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int screenHeight = activity.getWindow().getDecorView().getRootView().getHeight();
        return screenHeight - rect.bottom != 0;
    }

    /**
     * 设置沉浸式导航栏
     *
     * @param activity Activity实例
     */
    public static void setTranslucentNavigationBar(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 设置普通导航栏
     *
     * @param activity Activity实例
     */
    public static void setNormalNavigationBar(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 隐藏导航栏
     *
     * @param activity Activity实例
     */
    public static void hideNavigationBar(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        updateSystemUiVisibility(decorView, View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * 显示导航栏
     *
     * @param activity Activity实例
     */
    public static void showNavigationBar(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        updateSystemUiVisibility(decorView, View.SYSTEM_UI_FLAG_VISIBLE);
    }

    /**
     * 检查导航栏是否可见
     *
     * @param activity Activity实例
     * @return 是否可见
     */
    public static boolean isNavigationBarVisible(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        return (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
    }

    /**
     * 设置导航栏透明度
     *
     * @param activity     Activity实例
     * @param transparency 透明度值，范围为0.0到1.0
     */
    public static void setNavigationBarTransparency(@NonNull Activity activity, float transparency) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setNavigationBarColor(Color.argb((int) (transparency * 255), 0, 0, 0));
        }
    }

    /**
     * 设置导航栏颜色
     *
     * @param activity Activity实例
     * @param color    颜色值
     */
    public static void setNavigationBarColor(@NonNull Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setNavigationBarColor(color);
        }
    }

    /**
     * 获取主题中的导航栏颜色
     *
     * @param activity Activity实例
     * @return 导航栏颜色值
     */
    @ColorInt
    public static int getNavigationBarColorFromTheme(@NonNull Activity activity) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.navigationBarColor, typedValue, true);
        return typedValue.data;
    }

    /**
     * 更新系统UI可视性标志
     *
     * @param decorView  装饰视图
     * @param visibility 新的可视性标志
     */
    private static void updateSystemUiVisibility(View decorView, int visibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.setSystemUiVisibility(visibility);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }
}