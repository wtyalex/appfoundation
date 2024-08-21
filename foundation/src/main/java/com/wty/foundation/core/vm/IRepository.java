package com.wty.foundation.core.vm;

import com.wty.foundation.core.http.RetrofitManager;

public interface IRepository {
    static <T> T getApi(Class<T> clazz) {
        return RetrofitManager.getInstance().create(clazz);
    }
}