package com.wty.foundation.core.safe;

import android.os.Looper;

import androidx.lifecycle.LiveData;

/**
 * @author wutianyu
 * @createTime 2023/2/21 15:12
 * @describe 线程安全的LiveData，自动处理主线程和子线程的数据更新
 */
public class SafeMutableLiveData<T> extends LiveData<T> {
    public SafeMutableLiveData(T value) {
        super(value);
    }

    public SafeMutableLiveData() {}

    @Override
    public void setValue(T value) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            super.setValue(value);
        } else {
            postValue(value);
        }
    }
}
