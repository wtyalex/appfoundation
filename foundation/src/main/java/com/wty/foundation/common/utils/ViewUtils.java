package com.wty.foundation.common.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * @author wutianyu
 * @createTime 2024/9/3
 * @describe 视图工具类，提供视图查找、设置和操作等功能
 */
public class ViewUtils {
    private static final String TAG = "ViewUtils";

    private ViewUtils() {}

    /**
     * 从父视图中查找子视图
     *
     * @param parent 父视图
     * @param id 子视图的资源ID
     * @param <T> 视图的具体类型
     * @return 找到的子视图，如果找不到则返回null
     */
    @Nullable
    public static <T extends View> T findViewById(View parent, @IdRes int id) {
        if (parent == null) {
            return null;
        }
        return parent.findViewById(id);
    }

    /**
     * 获取视图的布局参数
     *
     * @param view 视图
     * @param <T> 布局参数的具体类型
     * @return 视图的布局参数，如果视图为空或者布局参数为空，则返回null
     */
    @Nullable
    public static <T extends ViewGroup.LayoutParams> T getLayoutParams(View view) {
        if (view == null) {
            return null;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            return null;
        }
        try {
            return (T)lp;
        } catch (ClassCastException e) {
            Log.e(TAG, "类型转换失败", e);
            return null;
        }
    }

    /**
     * 设置视图的内边距
     *
     * @param view 视图
     * @param left 左侧内边距
     * @param top 上侧内边距
     * @param right 右侧内边距
     * @param bottom 下侧内边距
     */
    public static void setPadding(View view, int left, int top, int right, int bottom) {
        if (view == null) {
            return;
        }
        view.setPadding(left, top, right, bottom);
    }

    /**
     * 设置视图的垂直方向内边距
     *
     * @param view 视图
     * @param top 上侧内边距
     * @param bottom 下侧内边距
     */
    public static void setVerticalPadding(View view, int top, int bottom) {
        if (view == null) {
            return;
        }
        setPadding(view, view.getPaddingLeft(), top, view.getPaddingRight(), bottom);
    }

    /**
     * 设置视图的垂直方向内边距，上下相同
     *
     * @param view 视图
     * @param padding 上下相同的内边距
     */
    public static void setVerticalPadding(View view, int padding) {
        setVerticalPadding(view, padding, padding);
    }

    /**
     * 设置视图的水平方向内边距
     *
     * @param view 视图
     * @param left 左侧内边距
     * @param right 右侧内边距
     */
    public static void setHorizontalPadding(View view, int left, int right) {
        if (view == null) {
            return;
        }
        setPadding(view, left, view.getPaddingTop(), right, view.getPaddingBottom());
    }

    /**
     * 设置视图的水平方向内边距，左右相同
     *
     * @param view 视图
     * @param padding 左右相同的内边距
     */
    public static void setHorizontalPadding(View view, int padding) {
        setHorizontalPadding(view, padding, padding);
    }

    /**
     * 设置TextView的文本资源ID
     *
     * @param view TextView
     * @param id 文本资源ID
     */
    public static <T extends TextView> void setText(T view, @StringRes int id) {
        if (view != null) {
            view.setText(id);
        }
    }

    /**
     * 设置TextView的文本
     *
     * @param view TextView
     * @param text 要设置的文本
     */
    public static <T extends TextView> void setText(T view, CharSequence text) {
        if (view != null) {
            view.setText(text);
        }
    }

    /**
     * 设置ImageView的图片资源ID
     *
     * @param view ImageView
     * @param id 图片资源ID
     */
    public static <T extends ImageView> void setImageResource(T view, @DrawableRes int id) {
        if (view != null) {
            view.setImageResource(id);
        }
    }

    /**
     * 设置ImageView的Bitmap资源
     *
     * @param view ImageView
     * @param bitmap Bitmap资源
     */
    public static <T extends ImageView> void setImageBitmap(T view, @Nullable Bitmap bitmap) {
        if (view != null) {
            view.setImageBitmap(bitmap);
        }
    }

    /**
     * 设置ImageView的Uri资源
     *
     * @param view ImageView
     * @param uri Uri资源
     */
    public static <T extends ImageView> void setImageURI(T view, @Nullable Uri uri) {
        if (view != null) {
            view.setImageURI(uri);
        }
    }
}
