package com.wty.foundation.core.base;

import java.lang.ref.WeakReference;

import com.wty.foundation.common.utils.StringUtils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

/**
 * @author wutianyu
 * @createTime 2023/2/13 14:16
 * @describe
 */
public class BaseTextWatcher implements TextWatcher {
    private String mOldText;
    protected WeakReference<TextView> reference;

    public BaseTextWatcher() {}

    public BaseTextWatcher(TextView textView) {
        this.reference = new WeakReference<>(textView);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mOldText = String.valueOf(s);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!isMatches(s.toString())) {
            TextView editText = reference.get();
            if (editText != null) {
                editText.setText(mOldText);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (StringUtils.isEqual(String.valueOf(s), mOldText)) {
            return;
        }
        afterTextChanged(String.valueOf(s));
    }

    protected void afterTextChanged(String s) {}

    protected boolean isMatches(String s) {
        return true;
    }
}
