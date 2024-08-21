package com.wty.foundation.core.base.activity;

import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.core.utils.DefaultActionBarUtils;
import com.wty.foundation.core.utils.ViewBindingUtils;
import com.wty.foundation.databinding.ActivityBaseBinding;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

/**
 * @author wutianyu
 * @createTime 2023/6/6 8:37
 * @describe
 */
public abstract class ActionBarActivity<VB extends ViewBinding> extends BaseActivity {
    private @Nullable ViewBinding mActionBarBinding;
    private @NonNull VB mViewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeView();
        mViewBinding = ViewBindingUtils.create(this.getClass(), getLayoutInflater());
        mActionBarBinding = getActionBarView();
        if (mActionBarBinding == null) {
            setContentView(mViewBinding.getRoot());
        } else {
            ActivityBaseBinding mBaseBinding = ActivityBaseBinding.inflate(getLayoutInflater());
            mBaseBinding.getRoot().addView(mActionBarBinding.getRoot(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mBaseBinding.getRoot().addView(mViewBinding.getRoot(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(mBaseBinding.getRoot());
        }
        initView();
    }

    @NonNull
    protected VB getViewBinding() {
        return mViewBinding;
    }

    protected final View.OnClickListener getOnClickListener() {
        return new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                ActionBarActivity.this.onClick(v);
            }
        };
    }

    protected @Nullable ViewBinding getActionBarView() {
        return DefaultActionBarUtils.getActionBar(getLayoutInflater());
    }

    protected void beforeView() {}

    protected abstract void initView();
}
