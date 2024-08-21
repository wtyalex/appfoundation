package com.wty.foundation.core.utils;

/**
 * @author wutianyu
 * @createTime 2023/1/30 19:02
 * @describe 用于任务回调
 */
public interface TaskResult<T> {
    /**
     * 任务结果回调
     * 
     * @param result 结果
     */
    void onResult(T result);
}
