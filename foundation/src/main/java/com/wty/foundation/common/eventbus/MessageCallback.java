package com.wty.foundation.common.eventbus;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe EventBus消息回调接口，定义消息处理方法
 */
public interface MessageCallback {
    void onMsg(MessageEvent msg);
}
