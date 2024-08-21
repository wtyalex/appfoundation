package com.wty.foundation.core.base.dialog;

import com.wty.foundation.R;
import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.core.utils.ViewBindingUtils;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewbinding.ViewBinding;

public abstract class BaseDialog<VB extends ViewBinding> extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "BaseDialog";
    private VB mViewBinding;
    private boolean isShowing;

    public BaseDialog() {
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            isShowing = savedInstanceState.getBoolean("isShowing", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.ui_dialog_base, container, false);
        mViewBinding = ViewBindingUtils.create(this.getClass(), inflater, rootView, true);
        rootView.setOnClickListener(new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                if (isCancelable()) {
                    dismiss();
                }
            }
        });
        return rootView.getRootView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog != null) {
            dialog.setOnDismissListener(dialog1 -> isShowing = false);
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isShowing", isShowing);
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    protected final VB getViewBinding() {
        return mViewBinding;
    }

    protected abstract void initView();

    public boolean isShowing() {
        return isShowing;
    }

    public void onDestroyView() {
        super.onDestroyView();
        mViewBinding = null;
        isShowing = false;
    }

    protected View.OnClickListener getOnClickListener() {
        return new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                BaseDialog.this.onClick(v);
            }
        };
    }

    public void show(FragmentManager manager) {
        show(manager, "BaseDialog");
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            super.show(manager, tag);
            isShowing = true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public int show(@NonNull FragmentTransaction transaction, @Nullable String tag) {
        try {
            int result = super.show(transaction, tag);
            isShowing = true;
            return result;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return -1;
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            super.showNow(manager, tag);
            isShowing = true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public void onClick(View v) {}
}