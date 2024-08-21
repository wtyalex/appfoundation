package com.wty.foundation.account;

import com.wty.foundation.R;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class GoToLoginDialog extends DialogFragment {
    private static final String TAG = "BaseDialog";
    private long lastTime = -1000;
    private TaskResult<Boolean> mTaskResult;

    public GoToLoginDialog() {
        setStyle(STYLE_NO_TITLE, R.style.ACFullScreenDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.dialog_accout_auth, container, false);
        rootView.findViewById(R.id.ok).setOnClickListener(v -> {
            long time = SystemClock.elapsedRealtime();
            if (time - lastTime > 800) {
                lastTime = time;
                if (mTaskResult != null) {
                    mTaskResult.onResult(true);
                    dismiss();
                }
            }
        });
        setCancelable(false);
        return rootView;
    }

    public void show(FragmentManager manager) {
        show(manager, "BaseDialog");
    }

    public void setTaskResult(TaskResult<Boolean> taskResult) {
        this.mTaskResult = taskResult;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            super.show(manager, tag);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}