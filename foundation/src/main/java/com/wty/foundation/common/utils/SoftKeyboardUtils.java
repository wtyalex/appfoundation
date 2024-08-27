package com.wty.foundation.common.utils;

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SoftKeyboardUtils {

    private static final String TAG = "SoftKeyboardUtils";
    private static final long DELAY_SHOW_KEYBOARD = 500L; // 延迟显示键盘的时间

    /**
     * 显示软键盘。
     *
     * @param context 上下文
     * @param editText EditText
     */
    public static void showSoftKeyboard(@NonNull Context context, @NonNull EditText editText) {
        if (editText == null || context == null) {
            Log.d(TAG, "showSoftKeyboard: editText or context is null");
            return;
        }
        editText.postDelayed(() -> {
            InputMethodManager inputMethodManager =
                (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager == null) {
                Log.d(TAG, "showSoftKeyboard: inputMethodManager is null");
                return;
            }
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
        }, DELAY_SHOW_KEYBOARD);
    }

    /**
     * 隐藏软键盘。
     *
     * @param context 上下文
     * @param view 当前获得焦点的视图
     */
    public static void hideSoftKeyboard(@NonNull Context context, @NonNull View view) {
        if (view == null || context == null) {
            Log.d(TAG, "hideSoftKeyboard: view or context is null");
            return;
        }
        InputMethodManager inputMethodManager =
            (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            Log.d(TAG, "hideSoftKeyboard: inputMethodManager is null");
            return;
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 检查软键盘是否可见。
     *
     * @param context 上下文
     * @return 如果软键盘可见返回 true
     */
    public static boolean isSoftKeyboardVisible(@NonNull Context context) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm != null && imm.isActive();
    }

    /**
     * 获取软键盘的高度。
     *
     * @param activity 活动
     * @return 软键盘的高度
     */
    public static int getSoftKeyboardHeight(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
        if (insets != null) {
            return insets.getSystemWindowInsetBottom();
        }
        return 0;
    }

    /**
     * 调整布局以适应键盘弹出。
     *
     * @param context 上下文
     * @param rootView 根布局视图
     * @param onKeyboardListener 键盘状态监听器
     */
    public static void adjustLayoutForKeyboard(@NonNull Context context, @NonNull View rootView,
        OnKeyboardListener onKeyboardListener) {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
            if (heightDiff > 100) { // 如果超过 100 像素，可能是键盘弹出了...
                if (onKeyboardListener != null) {
                    onKeyboardListener.onKeyboardShown();
                }
            } else {
                if (onKeyboardListener != null) {
                    onKeyboardListener.onKeyboardHidden();
                }
            }
        });
    }

    /**
     * 禁用复制和粘贴功能。
     *
     * @param editText EditText
     */
    public static void disableCopyAndPaste(@NonNull EditText editText) {
        if (editText == null) {
            Log.e(TAG, "disableCopyAndPaste: editText is null");
            return;
        }
        editText.setLongClickable(false);
        editText.setTextIsSelectable(false);
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // 当用户触摸视图时禁用插入
                setInsertionDisabled(editText);
            }
            return false;
        });

        // 禁用复制/粘贴的操作模式
        editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });
    }

    private static void setInsertionDisabled(@NonNull EditText editText) {
        try {
            Field editorField = TextView.class.getDeclaredField("mEditor");
            editorField.setAccessible(true);
            Object editorObject = editorField.get(editText);

            Class<?> editorClass = Class.forName("android.widget.Editor");
            Field mInsertionControllerEnabledField = editorClass.getDeclaredField("mInsertionControllerEnabled");
            mInsertionControllerEnabledField.setAccessible(true);
            mInsertionControllerEnabledField.set(editorObject, false);

            Field mSelectionControllerEnabledField = editorClass.getDeclaredField("mSelectionControllerEnabled");
            mSelectionControllerEnabledField.setAccessible(true);
            mSelectionControllerEnabledField.set(editorObject, false);
        } catch (Exception e) {
            Log.e(TAG, "设置插入和选择为禁用失败", e);
        }
    }

    /**
     * 判断软键盘是否应该被隐藏。
     *
     * @param v 视图
     * @param ev 触摸事件
     * @return 如果应该隐藏软键盘返回 true
     */
    public static boolean isShouldHideKeyboard(@NonNull View v, @NonNull MotionEvent ev) {
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
     * 键盘状态监听接口。
     */
    public interface OnKeyboardListener {
        void onKeyboardShown();

        void onKeyboardHidden();
    }

    /**
     * 辅助方法检查视图是否获得焦点并且触摸事件发生在视图外部。
     *
     * @param context 上下文
     * @param view 视图
     * @param touchEvent 触摸事件
     * @return 如果视图获得焦点并且触摸事件在视图外部则返回 true
     */
    public static boolean shouldHideKeyboardOnTouch(@NonNull Context context, @NonNull View view,
        @NonNull MotionEvent touchEvent) {
        if (view.isFocused()) {
            Rect r = new Rect();
            view.getGlobalVisibleRect(r);
            if (!r.contains((int)touchEvent.getRawX(), (int)touchEvent.getRawY())) {
                hideSoftKeyboard(context, view);
                return true;
            }
        }
        return false;
    }
}