package com.wty.foundation.common.eventbus;

public interface ISubscriber {
    ISubscriber setMsgHandler(MessageCallback callback);

    ISubscriber addAction(String action);

    ISubscriber setSticky(boolean isSticky);

    ISubscriber setPriority(int priority);

    ISubscriber register();

    void unregister();
}
