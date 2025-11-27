package com.wty.foundation.common.eventbus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe 后台线程事件订阅者，用于在后台线程处理事件消息
 */
class BackgroundSubscriber extends BaseSubscriber {
    private static final String TAG = "EventBus";

    @Subscribe(threadMode = ThreadMode.BACKGROUND, sticky = true)
    public void onBackground(MessageEvent event) {
        try {
            Log.i(TAG, "BackgroundSubscriber：onBackground:" + event.getAction());
            if (mActions.contains(event.getAction())) {
                onMessage(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "BackgroundSubscriber：onBackground", e);
        }

    }

    @Override
    ThreadMode getThreadMode() {
        return ThreadMode.BACKGROUND;
    }
}
