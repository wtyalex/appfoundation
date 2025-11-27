package com.wty.foundation.core.vm;

import com.wty.foundation.core.http.RetrofitManager;

/**
 * @author wutianyu
 * @createTime 2023/2/21 16:12
 * @describe 数据仓库接口，提供API接口实例创建功能
 */
public interface IRepository {
    static <T> T getApi(Class<T> clazz) {
        return RetrofitManager.getInstance().create(clazz);
    }

    static <T> T getApi(Class<T> clazz, String baseUrl) {
        return RetrofitManager.getInstance().create(clazz, baseUrl);
    }
}