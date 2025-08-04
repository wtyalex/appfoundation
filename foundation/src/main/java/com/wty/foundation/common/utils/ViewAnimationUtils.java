package com.wty.foundation.common.utils;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import java.lang.ref.WeakReference;

/**
 * Author: 吴天宇
 * Date: 2025/7/5 17:31
 * Description: 视图动画工具类
 */
public class ViewAnimationUtils {

    // 默认动画时长（毫秒）
    private static final long DEFAULT_DURATION = 200L;

    // 插值器实例
    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

    // 私有构造函数，防止实例化
    private ViewAnimationUtils() {

    }

    /**
     * 安全检测：视图是否可执行动画
     */
    private static boolean safeToAnimate(View view) {
        boolean isAttached;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            isAttached = view.isAttachedToWindow();
        } else {
            // API 19以下使用windowToken判断
            isAttached = view.getWindowToken() != null;
        }
        return isAttached && view.getWindowToken() != null && view.getVisibility() == View.VISIBLE;
    }

    /**
     * 安全执行操作（确保在主线程执行）
     */
    private static void safeExecute(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            new Handler(Looper.getMainLooper()).post(action);
        }
    }

    /**
     * 内存安全的Runnable封装
     */
    private static class SafeRunnable implements Runnable {
        private final WeakReference<View> weakView;
        private final ViewAction action;

        public SafeRunnable(View view, ViewAction action) {
            this.weakView = new WeakReference<>(view);
            this.action = action;
        }

        @Override
        public void run() {
            View view = weakView.get();
            if (view != null && safeToAnimate(view)) {
                action.execute(view);
            }
        }
    }

    /**
     * 视图操作接口
     */
    private interface ViewAction {
        void execute(View view);
    }

    /**
     * 基础点击动画（透明度变化）
     *
     * @param view     目标视图
     * @param duration 动画时长（毫秒）
     */
    public static void applyClickAnimation(View view, long duration) {
        if (!safeToAnimate(view)) return;

        // 终止可能存在的旧动画
        view.animate().cancel();

        view.animate().alpha(0.8f).setDuration(duration).setInterpolator(INTERPOLATOR).withEndAction(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new SafeRunnable(view, new ViewAction() {
                    @Override
                    public void execute(View v) {
                        v.animate().alpha(1.0f).setDuration(duration).setInterpolator(INTERPOLATOR).start();
                    }
                }), duration / 2);
            }
        }).start();
    }

    /**
     * 基础点击动画（透明度变化）
     *
     * @param view 目标视图
     */
    public static void applyClickAnimation(View view) {
        applyClickAnimation(view, DEFAULT_DURATION);
    }

    /**
     * 缩放点击动画（带按压效果）
     *
     * @param view     目标视图
     * @param scale    缩放比例（0.8f-1.0f）
     * @param duration 动画时长（毫秒）
     */
    public static void applyScaleAnimation(View view, float scale, long duration) {
        if (!safeToAnimate(view)) return;

        // 终止可能存在的旧动画
        view.animate().cancel();

        view.animate().scaleX(scale).scaleY(scale).setDuration(duration).setInterpolator(INTERPOLATOR).withEndAction(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new SafeRunnable(view, new ViewAction() {
                    @Override
                    public void execute(View v) {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(duration).setInterpolator(INTERPOLATOR).start();
                    }
                }), duration / 2);
            }
        }).start();
    }

    /**
     * 缩放点击动画（带按压效果）
     *
     * @param view  目标视图
     * @param scale 缩放比例（0.8f-1.0f）
     */
    public static void applyScaleAnimation(View view, float scale) {
        applyScaleAnimation(view, scale, DEFAULT_DURATION);
    }

    /**
     * 缩放点击动画（带按压效果）
     *
     * @param view 目标视图
     */
    public static void applyScaleAnimation(View view) {
        applyScaleAnimation(view, 0.95f, DEFAULT_DURATION);
    }

    /**
     * 背景色闪烁动画
     *
     * @param view     目标视图
     * @param color    闪烁的颜色
     * @param duration 动画时长（毫秒）
     */
    public static void applyColorFlashAnimation(View view, int color, long duration) {
        if (!safeToAnimate(view)) return;

        final android.graphics.drawable.Drawable originalBackground = view.getBackground();
        view.setBackgroundColor(color);

        new Handler(Looper.getMainLooper()).postDelayed(new SafeRunnable(view, new ViewAction() {
            @Override
            public void execute(View it) {
                it.setBackground(originalBackground);
            }
        }), duration);
    }

    /**
     * 背景色闪烁动画
     *
     * @param view  目标视图
     * @param color 闪烁的颜色
     */
    public static void applyColorFlashAnimation(View view, int color) {
        applyColorFlashAnimation(view, color, DEFAULT_DURATION);
    }

    /**
     * 组合动画（透明度+缩放）
     *
     * @param view     目标视图
     * @param scale    缩放比例（0.8f-1.0f）
     * @param duration 动画时长（毫秒）
     */
    public static void applyComboAnimation(View view, float scale, long duration) {
        if (!safeToAnimate(view)) return;

        // 终止可能存在的旧动画
        view.animate().cancel();

        view.animate().alpha(0.8f).scaleX(scale).scaleY(scale).setDuration(duration).setInterpolator(INTERPOLATOR).withEndAction(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new SafeRunnable(view, new ViewAction() {
                    @Override
                    public void execute(View v) {
                        v.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(duration).setInterpolator(INTERPOLATOR).start();
                    }
                }), duration / 2);
            }
        }).start();
    }

    /**
     * 组合动画（透明度+缩放）
     *
     * @param view  目标视图
     * @param scale 缩放比例（0.8f-1.0f）
     */
    public static void applyComboAnimation(View view, float scale) {
        applyComboAnimation(view, scale, DEFAULT_DURATION);
    }

    /**
     * 组合动画（透明度+缩放）
     *
     * @param view 目标视图
     */
    public static void applyComboAnimation(View view) {
        applyComboAnimation(view, 0.9f, DEFAULT_DURATION);
    }

    /**
     * 视图出现动画（从底部滑入）
     *
     * @param view     目标视图
     * @param duration 动画时长（毫秒）
     */
    public static void applyAppearAnimation(View view, long duration) {
        if (!safeToAnimate(view)) return;

        // 确保在视图布局完成后执行
        view.post(new Runnable() {
            @Override
            public void run() {
                if (!safeToAnimate(view)) return;

                view.animate().cancel();
                view.setTranslationY(view.getHeight());
                view.setAlpha(0f);
                view.setVisibility(View.VISIBLE);

                view.animate().translationY(0f).alpha(1f).setDuration(duration).setInterpolator(INTERPOLATOR).start();
            }
        });
    }

    /**
     * 视图出现动画（从底部滑入）
     *
     * @param view 目标视图
     */
    public static void applyAppearAnimation(View view) {
        applyAppearAnimation(view, 300L);
    }

    /**
     * 视图消失动画（滑出到底部）
     *
     * @param view     目标视图
     * @param duration 动画时长（毫秒）
     */
    public static void applyDisappearAnimation(View view, long duration) {
        if (!safeToAnimate(view)) return;

        view.animate().cancel();

        view.animate().translationY(view.getHeight()).alpha(0f).setDuration(duration).setInterpolator(INTERPOLATOR).withEndAction(new Runnable() {
            @Override
            public void run() {
                safeExecute(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.GONE);
                        // 重置状态以便下次显示
                        view.setTranslationY(0f);
                        view.setAlpha(1f);
                    }
                });
            }
        }).start();
    }

    /**
     * 视图消失动画（滑出到底部）
     *
     * @param view 目标视图
     */
    public static void applyDisappearAnimation(View view) {
        applyDisappearAnimation(view, 300L);
    }
}