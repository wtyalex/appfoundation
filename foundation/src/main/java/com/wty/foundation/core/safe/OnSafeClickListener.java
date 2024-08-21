package com.wty.foundation.core.safe;

import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class OnSafeClickListener implements OnClickListener {
    private static final int INTERVAL_MS = 800;
    private long lastTime = 0L;

    public void onClick(View v) {
        long nowTime = SystemClock.elapsedRealtime();
        if (nowTime - lastTime >= INTERVAL_MS) {
            onSafeClick(v);
            lastTime = nowTime;
        }
    }

    protected abstract void onSafeClick(View v);
}