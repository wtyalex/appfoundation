package com.wty.foundation.common.eventbus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.wty.foundation.common.utils.ArrayUtils;

import android.util.Log;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe EventBus超级订阅者，负责分发和管理各类事件消息
 */
final class SuperSubscriber {
    private static final String TAG = "EventBus";
    private final Map<String, List<BaseSubscriber>> mSubMap = new ConcurrentHashMap<>();

    private static class Instance {
        static final SuperSubscriber INSTANCE = new SuperSubscriber();
    }

    private SuperSubscriber() {
        EventBusGlobal.register(this);
    }

    static SuperSubscriber getInstance() {
        return Instance.INSTANCE;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMain(MessageEvent event) {
        try {
            Log.i(TAG, "SuperSubscriber：onMain:" + event.getAction());
            dispatchEvent(event, ThreadMode.MAIN);
        } catch (Exception e) {
            Log.e(TAG, "SuperSubscriber：onMain", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onBackground(MessageEvent event) {
        try {
            Log.i(TAG, "SuperSubscriber：onBackground:" + event.getAction());
            dispatchEvent(event, ThreadMode.BACKGROUND);
        } catch (Exception e) {
            Log.e(TAG, "SuperSubscriber：onBackground", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onAsync(MessageEvent event) {
        try {
            Log.i(TAG, "SuperSubscriber：onAsync:" + event.getAction());
            dispatchEvent(event, ThreadMode.ASYNC);
        } catch (Exception e) {
            Log.e(TAG, "SuperSubscriber：onAsync", e);
        }
    }

    private void dispatchEvent(MessageEvent event, ThreadMode threadMode) {
        if (event == null) {
            return;
        }
        if (mSubMap.containsKey(event.getAction())) {
            List<BaseSubscriber> subscribers = mSubMap.get(event.getAction());
            assert subscribers != null;
            for (BaseSubscriber sub : subscribers) {
                if (sub.getThreadMode() == threadMode) {
                    sub.onMessage(event);
                }
            }
        }
    }

    void register(BaseSubscriber subscriber, Set<String> actions) {
        if (subscriber == null || ArrayUtils.isEmpty(actions)) {
            return;
        }
        if (subscriber.isSticky()) {
            EventBusGlobal.register(subscriber);
            return;
        }
        Map<String, List<BaseSubscriber>> subMap = mSubMap;
        for (String action : actions) {
            List<BaseSubscriber> subs;
            if (subMap.containsKey(action)) {
                subs = subMap.get(action);
            } else {
                subs = new CopyOnWriteArrayList<>();
                subMap.put(action, subs);
            }
            subs.add(subscriber);
            Collections.sort(subs);
        }
    }

    void unregister(BaseSubscriber subscriber, Set<String> actions) {
        if (subscriber == null || ArrayUtils.isEmpty(actions)) {
            return;
        }
        if (subscriber.isSticky()) {
            EventBusGlobal.unregister(subscriber);
            return;
        }
        Map<String, List<BaseSubscriber>> subMap = mSubMap;
        for (String action : actions) {
            List<BaseSubscriber> subs = subMap.get(action);
            if (ArrayUtils.isEmpty(subs)) {
                break;
            }
            subs.remove(subscriber);
        }
    }
}
