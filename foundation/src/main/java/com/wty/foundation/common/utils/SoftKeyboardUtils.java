package com.wty.foundation.common.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

public class SoftKeyboardUtils {

    private static final String TAG = "SoftKeyboardUtils";
    private static volatile InputMethodManager sInputMethodManager;
    private static ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    private SoftKeyboardUtils() {}

    /**
     * 显示软键盘
     *
     * @param context 上下文
     * @param view 任何具有焦点的视图
     */
    public static void showSoftKeyboard(@NonNull Context context, @NonNull View view) {
        if (view == null || context == null) {
            Log.d(TAG, "showSoftKeyboard: view or context is null");
            return;
        }

        if (!view.isFocusable() || !view.isFocusableInTouchMode()) {
            Log.w(TAG, "showSoftKeyboard: The provided view is not focusable.");
            return;
        }

        if (!view.requestFocus()) {
            Log.w(TAG, "showSoftKeyboard: The provided view cannot gain focus.");
            return;
        }

        view.postDelayed(() -> {
            InputMethodManager inputMethodManager = getInputMethodManager(context);
            if (inputMethodManager == null) {
                Log.d(TAG, "showSoftKeyboard: inputMethodManager is null");
                return;
            }
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }, 500L);
    }

    /**
     * 隐藏软键盘
     *
     * @param context 上下文
     * @param view 当前获得焦点的视图
     */
    public static void hideSoftKeyboard(@NonNull Context context, @NonNull View view) {
        if (view == null || context == null) {
            Log.d(TAG, "hideSoftKeyboard: view or context is null");
            return;
        }
        InputMethodManager inputMethodManager = getInputMethodManager(context);
        if (inputMethodManager == null) {
            Log.d(TAG, "hideSoftKeyboard: inputMethodManager is null");
            return;
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 获取 InputMethodManager 实例
     *
     * @param context 上下文
     * @return InputMethodManager 实例
     */
    private static InputMethodManager getInputMethodManager(@NonNull Context context) {
        if (sInputMethodManager == null) {
            synchronized (SoftKeyboardUtils.class) {
                if (sInputMethodManager == null) {
                    sInputMethodManager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                }
            }
        }
        return sInputMethodManager;
    }

    /**
     * 检查软键盘是否打开
     *
     * @param context 上下文
     * @return 如果软键盘打开返回 true
     */
    public static boolean isKeyboardOpen(@NonNull Context context) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm != null && imm.isActive();
    }

    /**
     * 获取软键盘的高度
     *
     * @param view 视图
     * @return 软键盘的高度
     */
    public static int getSoftKeyboardHeight(@NonNull View view) {
        if (view == null) {
            return 0;
        }

        final Rect outRect = new Rect();
        view.getWindowVisibleDisplayFrame(outRect);
        int screenHeight = view.getRootView().getHeight();
        int heightDiff = screenHeight - (outRect.bottom - outRect.top);

        // 过滤掉屏幕底部导航栏高度
        Context appContext = view.getContext().getApplicationContext();
        WindowManager wm = (WindowManager)appContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();

        try {
            // 获取实际的屏幕高度（包括虚拟按键栏）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(dm);
            } else {
                display.getMetrics(dm);
            }
            int realHeight = dm.heightPixels;

            int usableHeight = outRect.bottom - outRect.top;
            int heightDifference = realHeight - usableHeight;

            // 如果高度差大于零，则认为键盘显示
            if (heightDifference > 0) {
                return heightDifference;
            }
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Failed to get real display metrics", e);
        }

        return heightDiff;
    }

    /**
     * 调整布局以适应键盘弹出
     *
     * @param context 上下文
     * @param rootView 根布局视图
     * @param threshold 阈值，用于判断键盘是否弹出
     * @param onKeyboardListener 键盘状态监听器
     */
    public static void adjustLayoutForKeyboard(@NonNull Context context, @NonNull View rootView, int threshold,
        OnKeyboardListener onKeyboardListener) {
        if (keyboardListener != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }

        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                if (heightDiff > threshold) { // 如果超过阈值，则认为键盘弹出了...
                    if (onKeyboardListener != null) {
                        onKeyboardListener.onKeyboardShown();
                    }
                } else {
                    if (onKeyboardListener != null) {
                        onKeyboardListener.onKeyboardHidden();
                    }
                }
            }
        };

        // 注意：在不再需要监听键盘状态变化时，请务必调用 removeListener 方法移除监听器，
        // 否则可能导致内存泄漏。
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
    }

    /**
     * 移除全局布局监听器。
     *
     * <p>
     * 在不再需要监听软键盘状态变化时，必须调用此方法以避免内存泄漏。
     * </p>
     *
     * @param rootView 根布局视图
     */
    public static void removeListener(@NonNull View rootView) {
        if (keyboardListener != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            keyboardListener = null;
        }
    }

    /**
     * 自动隐藏键盘当点击屏幕其他区域
     *
     * @param context 上下文
     * @param view 视图
     */
    public static void setupTapOutsideToDismissKeyboard(@NonNull Context context, @NonNull View view) {
        if (view == null || context == null) {
            Log.d(TAG, "setupTapOutsideToDismissKeyboard: view or context is null");
            return;
        }

        // 添加触摸事件监听器
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (isTouchOutsideInputArea(v, event)) {
                        hideSoftKeyboard(context, v);
                    }
                }
                return false;
            }

            private boolean isTouchOutsideInputArea(@NonNull View v, @NonNull MotionEvent ev) {
                if (v instanceof EditText) {
                    int[] location = new int[2]; // 局部变量
                    v.getLocationOnScreen(location);
                    int left = location[0];
                    int top = location[1];
                    int bottom = top + v.getHeight();
                    int right = left + v.getWidth();

                    return !(ev.getRawX() > left && ev.getRawX() < right && ev.getRawY() > top
                        && ev.getRawY() < bottom);
                }
                return false;
            }
        });

        // 在视图销毁时移除监听器
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {}

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.setOnTouchListener(null);
                v.removeOnAttachStateChangeListener(this);
            }
        });
    }

    /**
     * 判断触摸是否发生在输入框之外
     *
     * @param v 视图
     * @param ev 触摸事件
     * @return 如果触摸发生在输入框之外返回 true
     */
    private static boolean isTouchOutsideInputArea(@NonNull View v, @NonNull MotionEvent ev) {
        if (v instanceof EditText) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();

            return !(ev.getRawX() > left && ev.getRawX() < right && ev.getRawY() > top && ev.getRawY() < bottom);
        }
        return false;
    }

    /**
     * 键盘状态监听接口
     */
    public interface OnKeyboardListener {
        /**
         * 键盘显示时的处理
         */
        default void onKeyboardShown() {}

        /**
         * 键盘隐藏时的处理
         */
        default void onKeyboardHidden() {}
    }
}