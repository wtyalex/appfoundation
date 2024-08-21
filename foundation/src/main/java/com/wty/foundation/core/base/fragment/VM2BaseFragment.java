package com.wty.foundation.core.base.fragment;

import com.wty.foundation.core.vm.BaseViewModel;
import com.wty.foundation.core.vm.IRepository;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewbinding.ViewBinding;

public abstract class VM2BaseFragment<VM extends BaseViewModel<? extends IRepository>,
        VM2 extends BaseViewModel<? extends IRepository>, VB extends ViewBinding> extends
        VMBaseFragment<VM, VB> {
    private VM2 mViewModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mViewModel2 = new ViewModelProvider(getViewModel2StoreOwner()).get(getViewModelClass());
        } catch (Exception e) {
            Log.e("VMBaseFragment", Log.getStackTraceString(e));
        }
    }

    /**
     * 获取根据第二个ViewModel的Class
     *
     * @return Class<VM2>
     */
    protected abstract Class<VM2> getViewModelClass();

    /**
     * 创建ViewModel2所使用的ViewModelStoreOwner
     *
     * @return ViewModelStoreOwner 使用它管理ViewModel2生命周期,默认时Activity
     */
    protected ViewModelStoreOwner getViewModel2StoreOwner() {
        return getActivity();
    }

    /**
     * 获取第二个ViewModel
     *
     * @return ViewModel
     */
    protected final VM2 getViewModel2() {
        return mViewModel2;
    }
}