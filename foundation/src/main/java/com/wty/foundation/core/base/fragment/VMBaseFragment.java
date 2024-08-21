package com.wty.foundation.core.base.fragment;

import com.wty.foundation.common.utils.ReflectionUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.core.vm.BaseViewModel;
import com.wty.foundation.core.vm.IRepository;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewbinding.ViewBinding;

public abstract class VMBaseFragment<VM extends BaseViewModel<? extends IRepository>, VB extends ViewBinding>
    extends ActionBarFragment<VB> {
    private VM mViewModel;

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

    @Nullable
    @Override
    protected ViewBinding getActionBarView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return null;
    }

    /**
     * 获取 ViewModel实例
     * 
     * @return ViewModel
     */
    protected final VM getViewModel() {
        return mViewModel;
    }

    /**
     * 创建ViewModel所使用的ViewModelStoreOwner
     *
     * @return ViewModelStoreOwner 使用它管理ViewModel生命周期 默认Fragment自己
     */
    protected ViewModelStoreOwner getViewModelStoreOwner() {
        return this;
    }
}