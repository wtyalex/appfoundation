package com.wty.foundation.core.base.fragment;

import com.wty.foundation.core.base.dialog.LoadDialog;
import com.wty.foundation.core.fragment.VisibilityFragment;
import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.core.utils.TaskResult;
import com.wty.foundation.core.utils.ViewBindingUtils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

public abstract class BaseFragment<VB extends ViewBinding> extends VisibilityFragment implements View.OnClickListener {
    private static final String LOADING_DIALOG = "loading_dialog";
    private LoadDialog mLoadDialog;
    private VB mViewBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment loadDialog = getChildFragmentManager().findFragmentByTag(LOADING_DIALOG);
        if (loadDialog instanceof DialogFragment) {
            mLoadDialog = (LoadDialog)loadDialog;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        mViewBinding = ViewBindingUtils.create(this.getClass(), inflater, container, false);
        return mViewBinding.getRoot();
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    /**
     * 会强调
     * 
     * @param callback
     */
    public void onBackPressed(TaskResult<Boolean> callback) {
        if (callback != null) {
            callback.onResult(false);
        }
    }

    protected abstract void initView();

    protected final VB getViewBinding() {
        return mViewBinding;
    }

    /**
     * 显示加载等待框
     */
    protected void showLoadDialog(String msg) {
        if (mLoadDialog == null) {
            mLoadDialog = new LoadDialog();
        }
        mLoadDialog.setMsg(msg);
        if (!mLoadDialog.isShowing()) {
            mLoadDialog.show(getChildFragmentManager(), LOADING_DIALOG);
        }
    }

    /**
     * 关闭加载等待框
     */
    protected void closeLoadDialog() {
        if (mLoadDialog != null && mLoadDialog.isShowing()) {
            mLoadDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewBinding = null;
    }

    protected final View.OnClickListener getOnClickListener() {
        return new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                BaseFragment.this.onClick(v);
            }
        };
    }

    @Override
    public void onClick(View v) {}
}