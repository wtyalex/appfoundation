package com.wty.foundation.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.wty.foundation.common.init.AppContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Author: 吴天宇
 * Date: 2024/8/27 15:31
 * Description: 屏幕工具类，提供屏幕尺寸、密度、方向等信息及单位转换功能
 */
public class ScreenUtils {

    // 私有构造函数防止外部实例化
    private ScreenUtils() {
    }

    /**
     * 获取屏幕尺寸（英寸），比实际尺寸稍小
     *
     * @return 屏幕尺寸（英寸）
     */
    public static float calcScreenSize() {
        DisplayMetrics dm = getDisplayMetrics();
        return calcScreenSize(dm);
    }

    /**
     * 根据提供的显示度量计算屏幕尺寸（英寸）
     *
     * @param dm 显示度量对象
     * @return 屏幕尺寸（英寸）
     */
    public static float calcScreenSize(DisplayMetrics dm) {
        double w = dm.widthPixels / dm.xdpi;
        double h = dm.heightPixels / dm.ydpi;
        BigDecimal bg = new BigDecimal(Math.sqrt(w * w + h * h)).setScale(2, RoundingMode.HALF_UP);
        return bg.floatValue();
    }

    /**
     * 获取屏幕大小（去除导航栏后）
     *
     * @return 宽和高数组，第一个元素是宽度，第二个元素是高度
     */
    public static int[] getScreenSize() {
        DisplayMetrics dm = getDisplayMetrics();
        return new int[]{dm.widthPixels, dm.heightPixels};
    }

    /**
     * 获取屏幕方向
     *
     * @param context 上下文（建议传入Activity，若传入Application上下文可能返回默认方向）
     * @return 屏幕方向，如果无法确定则返回 -1
     */
    public static int getScreenOrientation(Context context) {
        if (context == null) {
            return -1;
        }
        if (context instanceof Activity) {
            return context.getResources().getConfiguration().orientation;
        } else {
            return context.getResources().getConfiguration().orientation;
        }
    }

    /**
     * 判断屏幕是否为横屏
     *
     * @param context 上下文（建议传入Activity）
     * @return 如果屏幕处于横屏模式，则返回 true；否则返回 false
     */
    public static boolean isLandscape(Context context) {
        return getScreenOrientation(context) == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 获取屏幕的物理尺寸（像素）
     *
     * @return 物理尺寸的宽高点对象
     */
    public static Point getPhysicalScreenSize() {
        WindowManager wm = getWindowManager();
        if (wm != null) {
            Point size = new Point();
            wm.getDefaultDisplay().getRealSize(size);
            return size;
        }
        return new Point(-1, -1); // 无法获取时返回无效值
    }

    /**
     * 获取屏幕宽度（以 dp 为单位）
     *
     * @return 屏幕宽度（dp）
     */
    public static int getScreenWidthInDp() {
        return convertPxToDp(getScreenWidthInPx());
    }

    /**
     * 获取屏幕高度（以 dp 为单位）
     *
     * @return 屏幕高度（dp）
     */
    public static int getScreenHeightInDp() {
        return convertPxToDp(getScreenHeightInPx());
    }

    /**
     * 获取屏幕宽度（以像素为单位）
     *
     * @return 屏幕宽度（像素）
     */
    public static int getScreenWidthInPx() {
        return getScreenSize()[0];
    }

    /**
     * 获取屏幕高度（以像素为单位）
     *
     * @return 屏幕高度（像素）
     */
    public static int getScreenHeightInPx() {
        return getScreenSize()[1];
    }

    /**
     * 获取屏幕密度
     *
     * @return 屏幕密度
     */
    public static float getScreenDensity() {
        return getDisplayMetrics().density;
    }

    /**
     * 获取屏幕密度 DPI
     *
     * @return 屏幕密度 DPI
     */
    public static int getScreenDensityDpi() {
        return getDisplayMetrics().densityDpi;
    }

    /**
     * 获取屏幕分辨率（宽 x 高）
     *
     * @return 分辨率字符串（例如 "1920x1080"）
     */
    public static String getScreenResolution() {
        Point size = getPhysicalScreenSize();
        return size.x + "x" + size.y;
    }

    /**
     * 将像素转换为 dp
     *
     * @param px 像素值
     * @return 转换后的dp值
     */
    private static int convertPxToDp(int px) {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        return (int) (px / displayMetrics.density + 0.5f);
    }

    /**
     * 获取显示度量对象
     *
     * @return 显示度量对象
     */
    private static DisplayMetrics getDisplayMetrics() {
        return AppContext.getInstance().getContext().getResources().getDisplayMetrics();
    }

    /**
     * 获取WindowManager服务
     *
     * @return WindowManager服务实例
     */
    private static WindowManager getWindowManager() {
        return (WindowManager) AppContext.getInstance().getContext().getSystemService(Context.WINDOW_SERVICE);
    }
}