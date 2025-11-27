package com.wty.foundation.common.eventbus;

import android.util.Log;

import androidx.collection.ArrayMap;

/**
 * @author wutianyu
 * @createTime 2024/8/24
 * @describe EventBus消息事件类，用于封装和传递事件数据
 */
public class MessageEvent {
    private final String action;
    private final ArrayMap<String, Object> mDataMap = new ArrayMap<>();

    public MessageEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void put(String key, Object object) {
        mDataMap.put(key, object);
    }

    public <T> T getValue(String key, T defValue) {
        try {
            return (T)mDataMap.get(key);
        } catch (Exception e) {
            Log.e("MessageEvent", "getValue:", e);
            return defValue;
        }
    }
}
