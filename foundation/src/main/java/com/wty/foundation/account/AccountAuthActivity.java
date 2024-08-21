package com.wty.foundation.account;

import java.util.List;

import com.wty.foundation.common.init.ActivityLifecycleManager;
import com.wty.foundation.common.utils.StringUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author wutianyu
 * @createTime 2023/9/27 9:05
 * @describe
 */
public class AccountAuthActivity extends AppCompatActivity {
    public static TaskResult<String> taskResult;
    private String mToken = "";
    private GoToLoginDialog mDialog;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskResult != null) {
            taskResult.onResult(mToken);
        }
        taskResult = null;
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        startLauncher();
    }

    private ActivityResultLauncher<Intent> mStartLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                mToken = result.getData().getStringExtra("token");
                if (taskResult != null) {
                    taskResult.onResult(mToken);
                }
            } else {
                if (taskResult != null) {
                    taskResult.onResult("");
                }
            }
            taskResult = null;

            exitApp();
        });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startLauncher();
    }

    private void startLauncher() {
        if (mDialog == null) {
            mDialog = new GoToLoginDialog();
            mDialog.setTaskResult(result -> {
                Intent intent = new Intent();
                if (StringUtils.isEqual(AccountAuthority.getInstance().getDeviceType(), "pad")) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.setClassName("com.xldz.smart.launcher",
                        "com.xldz.smart.launcher.app.activity.UnBackKillAppsLoginActivity");
                } else if (StringUtils.isEqual(AccountAuthority.getInstance().getDeviceType(), "phone")) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("NewToken", true);
                    intent.setClassName("com.xldz.phone.hostapp", "com.xldz.phone.hostapp.activity.LoginActivity");
                } else {
                    Log.e("AccountAuthActivity",
                        "mDeviceType is " + AccountAuthority.getInstance().getDeviceType() + ",is not pad or phone");
                    exitApp();
                    return;
                }

                try {
                    Log.e("AccountAuthActivity", "launch");
                    mStartLauncher.launch(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e("AccountAuthActivity", Log.getStackTraceString(e));
                    exitApp();
                }
            });
            mDialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private void exitApp() {
        List<Activity> activityList = ActivityLifecycleManager.getInstance().getActivities();
        for (Activity activity : activityList) {
            if (activity.isDestroyed() || activity.isFinishing()) {
                continue;
            }
            activity.finish();
        }
    }
}
