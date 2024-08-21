package com.wty.foundation.core.base.fragment;

import com.wty.foundation.databinding.ActivityBaseBinding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

/**
 * @author wutianyu
 * @createTime 2023/6/6 9:15
 * @describe
 */
public abstract class ActionBarFragment<VB extends ViewBinding> extends BaseFragment<VB> {
    private ViewBinding mActionBarBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        mActionBarBinding = getActionBarView(inflater, container);
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (mActionBarBinding == null) {
            return rootView;
        } else {
            ActivityBaseBinding mBaseBinding = ActivityBaseBinding.inflate(inflater, container, false);
            mBaseBinding.getRoot().addView(mActionBarBinding.getRoot(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mBaseBinding.getRoot().addView(rootView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return mBaseBinding.getRoot();
        }
    }

    protected @Nullable ViewBinding getActionBarView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return null;
    }
}
