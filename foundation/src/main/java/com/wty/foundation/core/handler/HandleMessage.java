package com.wty.foundation.core.handler;

import android.os.Message;

/**
 * @author wutianyu
 * @createTime 2023/1/15
 * @describe 消息处理接口，用于处理Handler发送的消息
 */
public interface HandleMessage {

    void processMessage(Message msg);
}
