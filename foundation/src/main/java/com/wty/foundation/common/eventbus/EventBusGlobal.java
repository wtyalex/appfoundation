package com.wty.foundation.common.eventbus;

import org.greenrobot.eventbus.EventBus;

public class EventBusGlobal {
    private EventBusGlobal() {}

    public static void post(MessageEvent event) {
        EventBus.getDefault().post(event);
    }

    public static void postSticky(MessageEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    public static ISubscriber createMain(MessageCallback callback) {
        ISubscriber subscriber = new MainSubscriber();
        subscriber.setMsgHandler(callback);
        return subscriber;
    }

    public static ISubscriber createBackground(MessageCallback callback) {
        ISubscriber subscriber = new BackgroundSubscriber();
        subscriber.setMsgHandler(callback);
        return subscriber;
    }

    public static ISubscriber createAsync(MessageCallback callback) {
        ISubscriber subscriber = new AsyncSubscriber();
        subscriber.setMsgHandler(callback);
        return subscriber;
    }

    static void register(Object obj) {
        EventBus.getDefault().register(obj);
    }

    static void unregister(Object obj) {
        EventBus.getDefault().unregister(obj);
    }
}
