package com.wty.foundation.account;

public interface TaskResult<T> {
    /**
     * 任务结果回调
     * 
     * @param result 结果
     */
    void onResult(T result);
}