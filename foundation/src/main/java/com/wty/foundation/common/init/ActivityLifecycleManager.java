package com.wty.foundation.common.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.wty.foundation.common.utils.ArrayUtils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ActivityLifecycleManager implements Application.ActivityLifecycleCallbacks {
    private int mResumeCount;
    private final ArrayList<Activity> mActivities = new ArrayList<>();
    private Activity mTopActivity;

    private ActivityLifecycleManager() {}

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mActivities.add(activity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mTopActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        mResumeCount++;
        mTopActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mTopActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mResumeCount--;
        if (mResumeCount <= 0) {
            mTopActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {
        mActivities.remove(activity);
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    public List<Activity> getActivities() {
        if (ArrayUtils.isEmpty(mActivities)) {
            return Collections.EMPTY_LIST;
        } else {
            return new ArrayList<>(mActivities);
        }
    }

    public boolean isForeground() {
        return mResumeCount > 0;
    }

    public Activity getTopActivity() {
        return mTopActivity;
    }

    private static class Instance {
        private static final ActivityLifecycleManager INSTANCE = new ActivityLifecycleManager();
    }

    public static ActivityLifecycleManager getInstance() {
        return Instance.INSTANCE;
    }
}
