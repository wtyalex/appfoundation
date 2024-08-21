package com.wty.foundation.common.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class ViewUtils {
    private ViewUtils() {}

    @Nullable
    public static <T extends View> T findViewById(View parent, @IdRes int id) {
        if (parent == null) {
            return null;
        }
        return parent.findViewById(id);
    }

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
        } catch (Exception e) {
            return null;
        }
    }

    public static void setPadding(View view, int left, int top, int right, int bottom) {
        if (view == null) {
            return;
        }
        view.setPadding(left, top, right, bottom);
    }

    public static void setVerticalPadding(View view, int top, int bottom) {
        if (view == null) {
            return;
        }
        setPadding(view, view.getPaddingLeft(), top, view.getPaddingRight(), bottom);
    }

    public static void setVerticalPadding(View view, int padding) {
        setVerticalPadding(view, padding, padding);
    }

    public static void setHorizontalPadding(View view, int left, int right) {
        if (view == null) {
            return;
        }
        setPadding(view, left, view.getPaddingTop(), right, view.getPaddingBottom());
    }

    public static void setHorizontalPadding(View view, int padding) {
        setHorizontalPadding(view, padding, padding);
    }

    public static <T extends TextView> void setText(T view, @StringRes int id) {
        if (view != null) {
            view.setText(id);
        }
    }

    public static <T extends TextView> void setText(T view, CharSequence charSequence) {
        if (view != null) {
            view.setText(charSequence);
        }
    }

    public static <T extends ImageView> void setImageResource(T view, @DrawableRes int id) {
        if (view != null) {
            view.setImageResource(id);
        }
    }
}
