package com.wty.foundation.common.utils;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class ClickUtils {
    // 私有构造函数防止外部实例化
    private ClickUtils() {}

    // 私有静态对象锁，用于同步操作
    private static final Object lock = new Object();
    // 上一次点击的时间戳
    private static long lastClickTime = 0;
    // 最小点击间隔时间（毫秒）
    private static final long MIN_CLICK_INTERVAL = 1000;

    /**
     * 扩展指定 View 的点击区域
     *
     * @param view 要扩展点击区域的 View
     * @param leftPadding 左侧额外的点击区域（单位：像素）
     * @param topPadding 顶部额外的点击区域（单位：像素）
     * @param rightPadding 右侧额外的点击区域（单位：像素）
     * @param bottomPadding 底部额外的点击区域（单位：像素）
     */
    public static void expandTouchArea(final View view, final int leftPadding, final int topPadding,
        final int rightPadding, final int bottomPadding) {
        final Rect delegateArea = new Rect();
        view.post(() -> {
            view.getHitRect(delegateArea);
            delegateArea.left -= leftPadding;
            delegateArea.top -= topPadding;
            delegateArea.right += rightPadding;
            delegateArea.bottom += bottomPadding;

            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                final ViewGroup parentViewGroup = (ViewGroup)parent;
                parentViewGroup.post(() -> {
                    TouchDelegate touchDelegate = new TouchDelegate(delegateArea, view);
                    parentViewGroup.setTouchDelegate(touchDelegate);
                });
            }
        });
    }

    /**
     * 检查是否允许点击
     *
     * @return 如果允许点击，则返回 true
     */
    public static boolean isClickAllowed() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastClickTime) > MIN_CLICK_INTERVAL) {
                lastClickTime = currentTime;
                return true;
            }
            return false;
        }
    }

    /**
     * 给 View 设置点击触觉反馈
     *
     * @param view 要设置点击反馈的 View
     */
    public static void setClickFeedback(View view) {
        view.setOnClickListener(v -> {
            if (isClickAllowed()) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });
    }

    /**
     * 设置点击透明度变化
     *
     * @param view 要设置点击透明度变化的 View
     * @param alphaOnPress 按下时的透明度
     * @param alphaOnRelease 释放时的透明度
     */
    public static void setClickAlphaChange(final View view, float alphaOnPress, float alphaOnRelease) {
        view.setOnClickListener(v -> {
            if (isClickAllowed()) {
                v.setAlpha(alphaOnPress);
                v.postDelayed(() -> {
                    if (v != null) {
                        v.setAlpha(alphaOnRelease);
                    }
                }, 100);
            }
        });
    }

    /**
     * 禁用点击
     *
     * @param view 要禁用点击的 View
     */
    public static void disableClick(final View view) {
        view.setEnabled(false);
        view.setClickable(false);
    }

    /**
     * 启用点击
     *
     * @param view 要启用点击的 View
     */
    public static void enableClick(final View view) {
        view.setEnabled(true);
        view.setClickable(true);
    }

    /**
     * 设置点击动画
     *
     * @param view 要设置点击动画的 View
     */
    public static void setClickAnimation(final View view) {
        Animation animation = new AlphaAnimation(0.5f, 1.0f);
        animation.setDuration(100);
        animation.setFillAfter(true);

        view.setOnClickListener(v -> {
            if (isClickAllowed()) {
                v.clearAnimation(); // 清除之前的动画
                v.startAnimation(animation);
            }
        });
    }

    /**
     * 设置点击延迟执行
     *
     * @param view 要设置点击延迟执行的 View
     * @param delayMillis 延迟的时间（单位：毫秒）
     */
    public static void setDelayedClick(final View view, long delayMillis) {
        final Handler handler = new Handler(Looper.getMainLooper());

        view.setOnClickListener(v -> {
            if (isClickAllowed()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 延迟后执行的操作
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 设置点击计数器
     *
     * @param view 要设置点击计数器的 View
     * @param clickCounter 点击计数器对象
     */
    public static void setClickCounter(final View view, final AtomicInteger clickCounter) {
        view.setOnClickListener(v -> {
            if (isClickAllowed()) {
                clickCounter.incrementAndGet();
            }
        });
    }

    /**
     * 防止快速重复点击
     *
     * @param view 要防止快速点击的 View
     * @param listener 要传递给 View 的点击监听器
     */
    public static void preventQuickClick(View view, View.OnClickListener listener) {
        view.setOnClickListener(v -> {
            synchronized (lock) {
                if (System.currentTimeMillis() - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = System.currentTimeMillis();
                    listener.onClick(v);
                }
            }
        });
    }

    /**
     * 单击和双击检测
     *
     * @param view 要处理单击和双击事件的 View
     * @param singleClickListener 单击监听器
     * @param doubleClickListener 双击监听器
     */
    public static void detectSingleAndDoubleTap(View view, View.OnClickListener singleClickListener,
        View.OnClickListener doubleClickListener) {
        GestureDetector gestureDetector =
            new GestureDetector(view.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (singleClickListener != null) {
                        singleClickListener.onClick(view);
                    }
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (doubleClickListener != null) {
                        doubleClickListener.onClick(view);
                    }
                    return true;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }
            });

        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    /**
     * 为 View 添加点击防抖功能
     *
     * @param view 要添加点击防抖的 View
     * @param listener 点击事件的监听器
     * @param debounceMillis 防抖时间（单位：毫秒）
     */
    public static void setDebouncedClickListener(final View view, final View.OnClickListener listener,
        final long debounceMillis) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final WeakReference<View> weakView = new WeakReference<>(view);

        view.setOnClickListener(v -> {
            handler.removeCallbacksAndMessages(null); // 移除所有回调
            handler.postDelayed(() -> {
                View currentView = weakView.get();
                if (currentView != null && listener != null) {
                    listener.onClick(currentView);
                }
            }, debounceMillis);
        });
    }
}