package com.wty.foundation.common.eventbus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe 主线程事件订阅者，用于在主线程处理事件消息
 */
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
