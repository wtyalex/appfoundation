package com.wty.foundation.common.eventbus;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe EventBus订阅者接口，定义订阅者的基本操作方法
 */
public interface ISubscriber {
    ISubscriber setMsgHandler(MessageCallback callback);

    ISubscriber addAction(String action);

    ISubscriber setSticky(boolean isSticky);

    ISubscriber setPriority(int priority);

    ISubscriber register();

    void unregister();
}
