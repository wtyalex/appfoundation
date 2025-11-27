package com.wty.foundation.core.base.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wty.foundation.R;
import com.wty.foundation.core.exception.AppCrashHandler;
import com.wty.foundation.core.safe.OnSafeClickListener;

/**
 * @author wutianyu
 * @createTime 2025/3/28
 * @describe 崩溃信息展示页面，用于显示异常信息并提供重启或退出选项
 */
public class CrashDisplayActivity extends AppCompatActivity {
    private static final String TAG = "CrashDisplayActivity";
    private String mCrashLog;
    private AppCrashHandler mCrashHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            finish();
            return;
        }
        mCrashHandler = AppCrashHandler.getInstance();
        setupWindowStyle();
        parseIntentData();
        showCrashDialog();
    }

    private void setupWindowStyle() {
        Window window = getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.5f;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
    }

    private void parseIntentData() {
        mCrashLog = getIntent().getStringExtra("crash_log");
        if (mCrashLog == null) mCrashLog = "No crash data available";
    }

    private void showCrashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CrashDialogStyle).setCancelable(false);
        if (mCrashHandler.isShowCrashInfo()) {
            builder.setTitle(R.string.dev_mode_title).setMessage(mCrashLog);
        } else {
            builder.setTitle(R.string.crash_title).setMessage(R.string.crash_message);
        }
        if (mCrashHandler.isShowRestart()) {
            builder.setPositiveButton(R.string.restart, null);
        }
        if (mCrashHandler.isShowCopy()) {
            builder.setNeutralButton(R.string.copy_log, null);
        }
        builder.setNegativeButton(R.string.exit, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            adjustDialogSize(dialog);
            setupDialogButtons(dialog);
        });
        dialog.show();
    }

    private void adjustDialogSize(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;

            params.width = (int) (screenWidth * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
    }

    private void setupDialogButtons(AlertDialog dialog) {
        if (mCrashHandler.isShowRestart()) {
            Button restartBtn = dialog.getButton(Dialog.BUTTON_POSITIVE);
            if (restartBtn != null) {
                restartBtn.setOnClickListener(new OnSafeClickListener() {
                    @Override
                    protected void onSafeClick(View v) {
                        handleRestart();
                        dialog.dismiss();
                    }
                });
            }
        }
        if (mCrashHandler.isShowCopy()) {
            Button copyBtn = dialog.getButton(Dialog.BUTTON_NEUTRAL);
            if (copyBtn != null) {
                copyBtn.setOnClickListener(new OnSafeClickListener() {
                    @Override
                    protected void onSafeClick(View v) {
                        copyToClipboard();
                    }
                });
            }
        }
        Button exitBtn = dialog.getButton(Dialog.BUTTON_NEGATIVE);
        if (exitBtn != null) {
            exitBtn.setOnClickListener(new OnSafeClickListener() {
                @Override
                protected void onSafeClick(View v) {
                    handleExit();
                    dialog.dismiss();
                }
            });
        }
    }

    private void handleRestart() {
        notifyBeforeRestart();
        Class<? extends Activity> restartClass = mCrashHandler.getRestartActivity();
        if (restartClass == null) {
            Toast.makeText(this, R.string.restart_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, restartClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishGracefully();
        mCrashHandler.exitProcess();
    }

    private void handleExit() {
        notifyBeforeExit();
        finishGracefully();
        mCrashHandler.exitProcess();
    }

    private void copyToClipboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("CrashLog", mCrashLog));
                    Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.log_copy_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                android.text.ClipboardManager cm = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setText(mCrashLog);
                    Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.log_copy_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.log_copy_failed, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Copy to clipboard failed", e);
        }
    }

    private void notifyBeforeRestart() {
        for (AppCrashHandler.CrashListener listener : mCrashHandler.getListeners()) {
            try {
                listener.beforeRestart();
            } catch (Exception e) {
                Log.w(TAG, "Listener error", e);
            }
        }
    }

    private void notifyBeforeExit() {
        for (AppCrashHandler.CrashListener listener : mCrashHandler.getListeners()) {
            try {
                listener.beforeExit();
            } catch (Exception e) {
                Log.w(TAG, "Listener error", e);
            }
        }
    }

    private void finishGracefully() {
        finish();
        overridePendingTransition(0, 0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}