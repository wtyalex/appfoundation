package com.wty.foundation.core.base.dialog;

import com.wty.foundation.common.utils.ReflectionUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.core.vm.BaseViewModel;
import com.wty.foundation.core.vm.IRepository;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewbinding.ViewBinding;

public abstract class VMBaseDialog<VM extends BaseViewModel<? extends IRepository>, VB extends ViewBinding>
    extends BaseDialog<VB> {
    private static final String LOADING_DIALOG = "loading_dialog";
    private VM mViewModel;
    private LoadDialog mLoadDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mViewModel =
                new ViewModelProvider(getViewModelStoreOwner()).get(ReflectionUtils.getVMClass(this.getClass()));
            mViewModel.observerLoadDialogState(this, showMsg -> {
                if (StringUtils.isNull(showMsg)) {
                    closeLoadDialog();
                } else {
                    showLoadDialog(showMsg);
                }
            });
        } catch (Exception e) {
            Log.e("VMBaseFragment", Log.getStackTraceString(e));
        }
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
            mLoadDialog.show(getParentFragmentManager(), LOADING_DIALOG);
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

    protected final VM getViewModel() {
        return mViewModel;
    }

    /**
     * 创建ViewModel所使用的ViewModelStoreOwner
     *
     * @return ViewModelStoreOwner 使用它管理ViewModel生命周期
     */
    protected ViewModelStoreOwner getViewModelStoreOwner() {
        return this;
    }
}