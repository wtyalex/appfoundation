package com.wty.foundation.core.base.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.wty.foundation.common.utils.StatusBarUtils;
import com.wty.foundation.core.base.dialog.LoadDialog;
import com.wty.foundation.core.utils.ResourceSetting;

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOADING_DIALOG = "loading_dialog";
    private LoadDialog mLoadDialog;
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarStyle();
        ResourceSetting.applyAdaptiveDensity(this, true);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Fragment loadDialog = getSupportFragmentManager().findFragmentByTag(LOADING_DIALOG);
        if (loadDialog instanceof DialogFragment) {
            mLoadDialog = (LoadDialog) loadDialog;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View currentFocus = getCurrentFocus();
            if (currentFocus instanceof EditText) {
                if (mInputMethodManager != null && shouldHideKeyboard(ev, currentFocus)) {
                    hideSoftInput(currentFocus.getWindowToken());
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判断是否应该隐藏键盘
     *
     * @param event 触摸事件
     * @param view  焦点View
     * @return 是否需要隐藏
     */
    private boolean shouldHideKeyboard(MotionEvent event, View view) {
        try {
            int[] location = {0, 0};
            view.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            int right = left + view.getWidth();
            int bottom = top + view.getHeight();

            // 触摸点的屏幕坐标
            float x = event.getRawX();
            float y = event.getRawY();

            // 检查触摸点是否在EditText外部
            return x < left || x > right || y < top || y > bottom;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 隐藏软键盘
     *
     * @param windowToken 窗口Token
     */
    private void hideSoftInput(IBinder windowToken) {
        if (windowToken == null) {
            return;
        }
        try {
            if (mInputMethodManager != null) {
                mInputMethodManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeLoadDialog();
        mInputMethodManager = null;
    }

    protected void setStatusBarStyle() {
        StatusBarUtils.setStatusBar(this, true, false, 0, true);
    }

    /**
     * 显示加载等待框
     */
    protected void showLoadDialog(String msg) {
        if (mLoadDialog == null) {
            mLoadDialog = new LoadDialog();
        }
        mLoadDialog.setMsg(msg);
        if (!mLoadDialog.isShowing()) {
            mLoadDialog.show(getSupportFragmentManager(), LOADING_DIALOG);
        }
    }

    /**
     * 关闭加载等待框
     */
    protected void closeLoadDialog() {
        if (mLoadDialog != null && mLoadDialog.isShowing()) {
            mLoadDialog.dismiss();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ResourceSetting.wrapContextFontScale(newBase));
    }
}