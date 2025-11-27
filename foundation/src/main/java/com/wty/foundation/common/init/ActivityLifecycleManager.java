package com.wty.foundation.common.init;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wty.foundation.common.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author wutianyu
 * @createTime 2024/8/21
 * @describe Activity生命周期管理器，负责跟踪应用中所有Activity的状态和生命周期
 */
public class ActivityLifecycleManager implements Application.ActivityLifecycleCallbacks {
    // 应用前台状态计数器：>0表示前台，=0表示后台
    private int mResumeCount;
    // 存活Activity列表
    private final CopyOnWriteArrayList<Activity> mActivities = new CopyOnWriteArrayList<>();
    // 当前栈顶Activity
    private volatile Activity mTopActivity;
    // 生命周期监听器列表
    private final CopyOnWriteArrayList<ActivityLifecycleCallback> mCallbacks = new CopyOnWriteArrayList<>();
    // 同步操作锁对象
    private final Object mLock = new Object();

    /**
     * 单例模式，使用静态内部类保证线程安全和懒加载
     */
    private static class InstanceHolder {
        private static final ActivityLifecycleManager INSTANCE = new ActivityLifecycleManager();
    }

    private ActivityLifecycleManager() {

    }

    /**
     * 获取单例实例
     *
     * @return 唯一的ActivityLifecycleManager实例
     */
    public static ActivityLifecycleManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Activity生命周期回调接口，供外部监听
     */
    public interface ActivityLifecycleCallback {
        default void onActivityCreated(@NonNull Activity activity) {
        }

        default void onActivityStarted(@NonNull Activity activity) {
        }

        default void onActivityResumed(@NonNull Activity activity) {
        }

        default void onActivityPaused(@NonNull Activity activity) {
        }

        default void onActivityStopped(@NonNull Activity activity) {
        }

        default void onActivityDestroyed(@NonNull Activity activity) {
        }

        default void onForegroundChanged(boolean isForeground) {
        }
    }

    /**
     * 注册生命周期回调监听
     *
     * @param callback 要注册的回调接口
     */
    public void registerCallback(@NonNull ActivityLifecycleCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * 移除生命周期回调监听
     *
     * @param callback 要移除的回调接口
     */
    public void unregisterCallback(@NonNull ActivityLifecycleCallback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * 获取当前所有存活的Activity列表
     *
     * @return 不可修改的Activity列表
     */
    public List<Activity> getActivities() {
        return Collections.unmodifiableList(mActivities);
    }

    /**
     * 判断应用是否处于前台状态
     *
     * @return true: 前台状态, false: 后台状态
     */
    public boolean isForeground() {
        synchronized (mLock) {
            return mResumeCount > 0;
        }
    }

    /**
     * 获取当前栈顶Activity
     *
     * @return 栈顶Activity, 可能为null(应用在后台时)
     */
    @Nullable
    public Activity getTopActivity() {
        return mTopActivity;
    }

    /**
     * 检查指定Activity是否存活
     *
     * @param activity 要检查的Activity
     * @return true: 存活, false: 已销毁
     */
    public boolean isActivityAlive(@NonNull Activity activity) {
        return mActivities.contains(activity) && !activity.isFinishing() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed());
    }

    /**
     * 关闭所有Activity
     */
    public void finishAllActivities() {
        for (Activity activity : new ArrayList<>(mActivities)) {
            if (isActivityAlive(activity)) {
                activity.finish();
            }
        }
    }

    /**
     * 关闭指定Activity之外的所有Activity
     *
     * @param excludeActivity 要保留的Activity
     */
    public void finishAllActivitiesExcept(@NonNull Activity excludeActivity) {
        for (Activity activity : new ArrayList<>(mActivities)) {
            if (!activity.equals(excludeActivity) && isActivityAlive(activity)) {
                activity.finish();
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mActivities.add(activity);
        mTopActivity = activity;

        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityCreated(activity);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        synchronized (mLock) {
            mResumeCount++;
            if (mResumeCount == 1) {
                for (ActivityLifecycleCallback callback : mCallbacks) {
                    callback.onForegroundChanged(true);
                }
            }
        }
        mTopActivity = activity;

        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityStarted(activity);
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mTopActivity = activity;

        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityResumed(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        boolean wasForeground;
        synchronized (mLock) {
            wasForeground = mResumeCount > 0;
            mResumeCount--;
        }

        if (wasForeground && !isForeground()) {
            for (ActivityLifecycleCallback callback : mCallbacks) {
                callback.onForegroundChanged(false);
            }
            mTopActivity = null;
        }

        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityStopped(activity);
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mActivities.remove(activity);

        if (activity.equals(mTopActivity)) {
            mTopActivity = ArrayUtils.isEmpty(mActivities) ? null : mActivities.get(mActivities.size() - 1);
        }

        for (ActivityLifecycleCallback callback : mCallbacks) {
            callback.onActivityDestroyed(activity);
        }
    }
}