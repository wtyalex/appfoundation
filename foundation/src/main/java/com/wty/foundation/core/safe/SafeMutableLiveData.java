package com.wty.foundation.core.safe;

import android.os.Looper;

import androidx.lifecycle.LiveData;

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
