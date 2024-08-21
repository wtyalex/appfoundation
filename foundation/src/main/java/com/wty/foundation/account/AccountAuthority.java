package com.wty.foundation.account;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.wty.foundation.common.init.AppContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2023/9/27 9:00
 * @describe
 */
public class AccountAuthority implements TaskResult<String> {
    private AtomicBoolean isRequested = new AtomicBoolean(false);
    private CopyOnWriteArrayList<TaskResult<String>> listeners = new CopyOnWriteArrayList<>();
    private TaskResult<String> mCallback;
    private String mDeviceType;

    private AccountAuthority() {
        SharedPreferences sp = AppContext.getInstance().getContext().getSharedPreferences("host", Context.MODE_PRIVATE);
        mDeviceType = sp.getString("device_type", "pad");
        Log.i("wty", "mDeviceType:" + mDeviceType);
    }

    public String getDeviceType() {
        return mDeviceType;
    }

    @Override
    public void onResult(String result) {
        isRequested.set(false);
        if (this.mCallback != null) {
            mCallback.onResult(result);
        }
        for (TaskResult<String> taskResult : listeners) {
            taskResult.onResult(result);
        }
        listeners.clear();
    }

    private static class Instance {
        private static final AccountAuthority INSTANCE = new AccountAuthority();
    }

    public static AccountAuthority getInstance() {
        return Instance.INSTANCE;
    }

    public void setCallback(TaskResult<String> mCallback) {
        this.mCallback = mCallback;
    }

    public void startAccountActivity(TaskResult<String> taskResult) {
        long start = SystemClock.elapsedRealtime();
        listeners.add(taskResult);
        if (isRequested.compareAndSet(false, true)) {
            AccountAuthActivity.taskResult = this;
            Context context = AppContext.getInstance().getContext();
            Intent intent = new Intent(context, AccountAuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        Log.i("TokenInterceptor", String.valueOf(SystemClock.elapsedRealtime() - start));
    }
}
