package com.wty.foundation.core.base.dialog;

import com.wty.foundation.R;
import com.wty.foundation.common.utils.ResUtils;
import com.wty.foundation.common.utils.ScreenUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.common.utils.ViewUtils;
import com.wty.foundation.databinding.UiDialogBaseAlertBinding;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

/**
 * @author wutianyu
 * @createTime 2023/2/11 16:37
 * @describe
 */
public abstract class BaseAlertDialog<VB extends ViewBinding> extends BaseDialog<VB> {
    private OnAlertDialogListener mDialogListener;
    private UiDialogBaseAlertBinding baseAlertBinding;
    private String button1 = "取消";
    private String button2 = "确定";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup)super.onCreateView(inflater, container, savedInstanceState);
        root.removeView(getViewBinding().getRoot());
        baseAlertBinding = UiDialogBaseAlertBinding.inflate(inflater, root, true);
        baseAlertBinding.contentLayout.addView(getViewBinding().getRoot());
        baseAlertBinding.cancel.setOnClickListener(v -> {
            if (mDialogListener != null) {
                if (!mDialogListener.onButton1Click()) {
                    dismiss();
                }
            } else {
                dismiss();
            }
        });
        baseAlertBinding.ok.setOnClickListener(v -> {
            if (mDialogListener != null) {
                if (!mDialogListener.onButton2Click()) {
                    dismiss();
                }
            } else {
                dismiss();
            }
        });
        baseAlertBinding.cancel.setText(button1);
        baseAlertBinding.ok.setText(button2);
        ViewGroup.LayoutParams lp = ViewUtils.getLayoutParams(getViewBinding().getRoot());
        if (lp != null) {
            if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                lp.width = ScreenUtils.getScreenSize()[0] * 2 / 5;
            } else {
                lp.width = ScreenUtils.getScreenSize()[0] - getHorizontalMargin() * 2;
            }
        }
        getViewBinding().getRoot().setLayoutParams(lp);
        return root;
    }

    /**
     * 对话框离屏幕的水平间距
     * 
     * @return int 默认36dp R.dimen.base_alert_dialog_horizontal_margin
     */
    protected int getHorizontalMargin() {
        return ResUtils.getDimensionPixelSize(R.dimen.base_alert_dialog_horizontal_margin);
    }

    public void setDialogListener(OnAlertDialogListener dialogListener) {
        this.mDialogListener = dialogListener;
    }

    public void setButton1(String button1) {
        if (StringUtils.isNullEmpty(button1)) {
            return;
        }
        this.button1 = button1;
        if (baseAlertBinding != null) {
            baseAlertBinding.cancel.setText(button1);
        }
    }

    public void setButton2(String button2) {
        if (StringUtils.isNullEmpty(button2)) {
            return;
        }
        this.button2 = button2;
        if (baseAlertBinding != null) {
            baseAlertBinding.ok.setText(button2);
        }
    }

    public interface OnAlertDialogListener {
        default boolean onButton1Click() {
            return false;
        }

        boolean onButton2Click();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseAlertBinding = null;
    }
}
