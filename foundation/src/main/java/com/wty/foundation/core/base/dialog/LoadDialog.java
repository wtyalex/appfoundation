package com.wty.foundation.core.base.dialog;

import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.databinding.UiDialogLoadBinding;

import android.view.View;

/**
 * @author wutianyu
 * @createTime 2023/1/30 19:28
 * @describe 加载提示对话框，用于显示加载状态和提示信息
 */
public class LoadDialog extends BaseDialog<UiDialogLoadBinding> {
    private String mHintText;

    @Override
    protected void initView() {
        setCancelable(false);
        updateUI(mHintText);
    }

    public void setMsg(String msg) {
        mHintText = msg;
        updateUI(msg);
    }

    private void updateUI(String msg) {
        UiDialogLoadBinding binding = getViewBinding();
        if (binding != null) {
            binding.msg.setText(mHintText);
            binding.msg.setVisibility(StringUtils.isNullEmpty(msg) ? View.GONE : View.VISIBLE);
        }
    }

}
