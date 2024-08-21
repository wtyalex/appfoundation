package com.wty.foundation.core.base.activity;

import com.wty.foundation.common.utils.ReflectionUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.core.vm.BaseViewModel;
import com.wty.foundation.core.vm.IRepository;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

public abstract class VMBaseActivity<VM extends BaseViewModel<? extends IRepository>, VB extends ViewBinding>
    extends ActionBarActivity<VB> {
    private @NonNull VM mViewModel;

    @CallSuper
    @Override
    protected void beforeView() {
        mViewModel = new ViewModelProvider(this).get(ReflectionUtils.getVMClass(this.getClass()));
        mViewModel.observerLoadDialogState(this, showMsg -> {
            if (StringUtils.isNull(showMsg)) {
                closeLoadDialog();
            } else {
                showLoadDialog(showMsg);
            }
        });
    }

    protected final @NonNull VM getViewModel() {
        return mViewModel;
    }
}