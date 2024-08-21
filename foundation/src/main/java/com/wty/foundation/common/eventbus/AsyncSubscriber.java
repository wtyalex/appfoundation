package com.wty.foundation.common.eventbus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.util.Log;

class AsyncSubscriber extends BaseSubscriber {
    private static final String TAG = "EventBus";

    @Subscribe(threadMode = ThreadMode.ASYNC, sticky = true)
    public void onAsync(MessageEvent event) {
        try {
            Log.i(TAG, "AsyncSubscriber:onAsync:" + event.getAction());
            if (mActions.contains(event.getAction())) {
                onMessage(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "AsyncSubscriber:onAsync", e);
        }
    }

    @Override
    ThreadMode getThreadMode() {
        return ThreadMode.ASYNC;
    }
}
