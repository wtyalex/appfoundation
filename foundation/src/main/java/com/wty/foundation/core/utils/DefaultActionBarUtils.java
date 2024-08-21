package com.wty.foundation.core.utils;

import com.wty.foundation.R;
import com.wty.foundation.common.utils.ResUtils;
import com.wty.foundation.common.utils.ViewUtils;
import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.databinding.UiActionBarItiBinding;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author wutianyu
 * @createTime 2023/6/6 8:54
 * @describe
 */
public class DefaultActionBarUtils {
    private DefaultActionBarUtils() {}

    public static UiActionBarItiBinding getActionBar(@NonNull LayoutInflater inflater) {
        return getActionBar(inflater, null);
    }

    public static UiActionBarItiBinding getActionBar(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        UiActionBarItiBinding binding = container == null ? UiActionBarItiBinding.inflate(inflater)
            : UiActionBarItiBinding.inflate(inflater, container, false);
        final Context context = inflater.getContext();
        if (context instanceof Activity) {
            binding.actionBarLeft.setOnClickListener(new OnSafeClickListener() {
                @Override
                protected void onSafeClick(View v) {
                    ((Activity)context).onBackPressed();
                }
            });
        }
        binding.actionBarLeft.setImageResource(R.drawable.black_back);
        binding.actionBarLeft.setScaleType(ImageView.ScaleType.FIT_START);
        ViewUtils.setVerticalPadding(binding.actionBarLeft,
            ResUtils.getDimensionPixelSize(R.dimen.back_icon_vertical_padding));
        ViewUtils.setHorizontalPadding(binding.actionBarLeft,
            ResUtils.getDimensionPixelSize(R.dimen.action_bar_horizontal_padding), 0);
        binding.actionBarRight.setScaleType(ImageView.ScaleType.FIT_END);
        ViewUtils.setVerticalPadding(binding.actionBarRight,
            ResUtils.getDimensionPixelSize(R.dimen.back_icon_vertical_padding));
        ViewUtils.setHorizontalPadding(binding.actionBarRight, 0,
            ResUtils.getDimensionPixelSize(R.dimen.action_bar_horizontal_padding));
        return binding;
    }
}
