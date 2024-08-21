package com.wty.foundation.common.init;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * 上下文环境类
 */
public class AppContext {
    private Context mContext;

    private static class Instance {
        private static final AppContext INSTANCE = new AppContext();
    }

    private AppContext() {}

    /**
     * 单例获取AppContext对象
     *
     * @return AppContext
     */
    public static AppContext getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * 初始化 Application
     */
    public void initContext(Application context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
            context.registerActivityLifecycleCallbacks(ActivityLifecycleManager.getInstance());
        }

    }

    /**
     * 获取 Context 如果这个方法在initApplication之前调用，则会抛出IllegalStateException异常
     *
     * @return Context
     */
    @NonNull
    public Context getContext() {
        if (mContext == null) {
            throw new IllegalStateException("please call after initApplication()");
        }
        return mContext;
    }
}
