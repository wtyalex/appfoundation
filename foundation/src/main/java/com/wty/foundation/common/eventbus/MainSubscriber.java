package com.wty.foundation.common.eventbus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.util.Log;

class MainSubscriber extends BaseSubscriber {
    private static final String TAG = "EventBus";

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onMain(MessageEvent event) {
        try {
            Log.i(TAG, "MainSubscriber：onMain:" + event.getAction());
            if (mActions.contains(event.getAction())) {
                onMessage(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "MainSubscriber：onMain", e);
        }
    }

    @Override
    ThreadMode getThreadMode() {
        return ThreadMode.MAIN;
    }
}
