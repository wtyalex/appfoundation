package com.wty.foundation.core.handler;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

public class WeakHandler extends Handler {
    private final WeakReference<HandleMessage> mHandle;

    public WeakHandler(HandleMessage handle) {
        this.mHandle = new WeakReference<>(handle);
    }

    public WeakHandler(@NonNull Looper looper, HandleMessage handle) {
        super(looper);
        this.mHandle = new WeakReference<>(handle);
    }

    @Override
    public final void handleMessage(@NonNull Message msg) {
        HandleMessage handle = mHandle.get();
        if (handle != null) {
            handle.processMessage(msg);
        }
    }
}
