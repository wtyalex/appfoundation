package com.wty.foundation.core.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2023/6/7 14:55
 * @describe
 */
public class ResourceSetting {
    private static final String TAG = "ResourceSetting";
    private static float sNonCompatDensity;
    private static float sNonCompatScaledDensity;

    private ResourceSetting() {
    }

    /**
     * 初始化适配参数
     *
     * @param application 应用上下文
     */
    public static void init(Application application) {
        try {
            final DisplayMetrics dm = application.getResources().getDisplayMetrics();
            if (sNonCompatDensity == 0) {
                sNonCompatDensity = dm.density;
                sNonCompatScaledDensity = dm.scaledDensity;

                application.registerComponentCallbacks(new ComponentCallbacks() {
                    @Override
                    public void onConfigurationChanged(Configuration newConfig) {
                        if (newConfig != null && newConfig.fontScale > 0) {
                            sNonCompatScaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
                        }
                    }

                    @Override
                    public void onLowMemory() {
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化失败", e);
        }
    }

    /**
     * 为Activity应用适配密度
     *
     * @param activity     目标Activity
     * @param fixFontScale 是否固定字体缩放为1.0
     */
    public static void applyAdaptiveDensity(Activity activity, boolean fixFontScale) {
        if (activity == null) return;

        try {
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            Configuration config = activity.getResources().getConfiguration();

            int swDp = Math.min(config.screenWidthDp, config.screenHeightDp);
            int baseWidthDp;

            if (swDp < 320) baseWidthDp = 280;      // 手表
            else if (swDp < 360) baseWidthDp = 360; // 小屏手机
            else if (swDp < 420) baseWidthDp = 375; // 普通手机
            else if (swDp < 600) baseWidthDp = 414; // 大屏手机
            else if (swDp < 720) baseWidthDp = 600; // 小平板
            else if (swDp < 1080) baseWidthDp = 720; // 大平板/TV
            else baseWidthDp = 1080;                 // 超大屏/8K TV

            float targetDensity = (float) dm.widthPixels / baseWidthDp;
            targetDensity = Math.max(0.8f, Math.min(targetDensity, 4.0f));

            float targetScaledDensity = targetDensity * (sNonCompatScaledDensity / sNonCompatDensity);
            int targetDensityDpi = (int) (160 * targetDensity);

            dm.density = targetDensity;
            dm.scaledDensity = targetScaledDensity;
            dm.densityDpi = targetDensityDpi;

            if (fixFontScale && config.fontScale != 1f) {
                Configuration newConfig = new Configuration(config);
                newConfig.fontScale = 1f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    activity.applyOverrideConfiguration(newConfig);
                } else {
                    activity.getResources().updateConfiguration(newConfig, dm);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                    return insets.consumeSystemWindowInsets();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "适配失败", e);
        }
    }

    /**
     * 包装上下文以固定字体缩放
     *
     * @param context 原始上下文
     * @return 处理后的上下文
     */
    public static Context wrapContextFontScale(Context context) {
        if (context == null) return null;

        try {
            Configuration config = context.getResources().getConfiguration();
            if (config.fontScale != 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Configuration newConfig = new Configuration(config);
                newConfig.fontScale = 1f;
                return context.createConfigurationContext(newConfig);
            }
        } catch (Exception e) {
            Log.e(TAG, "包装上下文失败", e);
        }
        return context;
    }
}