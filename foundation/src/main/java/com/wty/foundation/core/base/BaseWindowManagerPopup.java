package com.wty.foundation.core.base;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.wty.foundation.common.init.AppContext;

import java.lang.ref.WeakReference;

/**
 * @author wutianyu
 * @createTime 2025/8/13 18:16
 * @describe: 独立于Activity生命周期的WindowManager弹窗基类
 */
public abstract class BaseWindowManagerPopup {
    private static final String TAG = "BaseWindowManagerPopup";

    // 上下文引用
    protected final WeakReference<Context> mAppContextRef;
    // WindowManager实例
    protected WindowManager mWindowManager;
    // 窗口参数（根据系统版本自动适配）
    protected WindowManager.LayoutParams mWindowParams;

    // 视图相关
    protected FrameLayout mRootView;
    protected View mContentView;
    protected View mBackgroundView;

    // 状态管理
    protected boolean isShowing;
    protected boolean isDismissing;
    protected boolean mCancelable = true;
    protected boolean mCancelableOnTouchOutside = true;
    protected boolean mBlockBackgroundTouch = true;

    // 动画相关
    protected Animation mEnterAnimation;
    protected Animation mExitAnimation;
    protected Animation mBackgroundEnterAnim;
    protected Animation mBackgroundExitAnim;
    protected int mEnterAnimResId = -1;
    protected int mExitAnimResId = -1;
    protected int mBackgroundEnterAnimResId = -1;
    protected int mBackgroundExitAnimResId = -1;

    // 触摸与拖拽
    protected float mDownX;
    protected float mDownY;
    protected float mInitialX;
    protected float mInitialY;
    protected boolean mIsDragging;
    protected int mTouchSlop;
    protected boolean mIsTouchInside;
    protected boolean mDraggable = false;

    // 屏幕信息
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected int mStatusBarHeight;

    // 自动消失
    private final Runnable mAutoDismissRunnable = this::dismiss;
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // 回调接口
    protected OnDismissListener mOnDismissListener;
    protected OnShowListener mOnShowListener;
    protected OnKeyListener mOnKeyListener;
    protected OnTouchOutsideListener mOnTouchOutsideListener;

    /**
     * 构造方法
     */
    public BaseWindowManagerPopup() {
        this.mAppContextRef = new WeakReference<>(AppContext.getInstance().getContext().getApplicationContext());
        initWindowManager();
        initScreenInfo();
        initStatusBarHeight();
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        initWindowParams();
        initRootView();
        initBackgroundView();
        initContentView();
        setupViews();
        setupEvents();
    }

    /**
     * 获取上下文
     */
    @Nullable
    protected Context getContext() {
        return mAppContextRef.get();
    }

    /**
     * 检查是否拥有悬浮窗权限
     */
    public boolean hasOverlayPermission() {
        Context context = getContext();
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return android.provider.Settings.canDrawOverlays(context);
            } catch (Exception e) {
                Log.e(TAG, "检查权限时出错", e);
                return false;
            }
        }
        return true; // Android 6.0 以下默认有权限
    }

    /**
     * 初始化WindowManager
     */
    protected void initWindowManager() {
        Context context = getContext();
        if (context != null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        } else {
            Log.e(TAG, "初始化失败：Application Context为空");
        }
    }

    /**
     * 初始化屏幕信息
     */
    protected void initScreenInfo() {
        if (mWindowManager == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Android 4.2+ 支持获取真实屏幕尺寸
            mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            // 低版本获取可见屏幕尺寸
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
        }
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    /**
     * 初始化状态栏高度
     */
    protected void initStatusBarHeight() {
        Context context = getContext();
        if (context == null) return;

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
    }

    /**
     * 初始化窗口参数
     */
    protected void initWindowParams() {
        mWindowParams = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 使用TYPE_APPLICATION_OVERLAY
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4-7.1 使用TYPE_TOAST（减少权限要求）
            mWindowParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            // Android 4.0-4.3 使用TYPE_PHONE
            mWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 通用窗口属性
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        // 低版本兼容性处理
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            mWindowParams.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        // 位置与大小
        mWindowParams.gravity = Gravity.CENTER;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.x = 0;
        mWindowParams.y = 0;

        // 全面屏（刘海屏）适配（Android 10+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mWindowParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    /**
     * 初始化根视图
     */
    protected void initRootView() {
        Context context = getContext();
        if (context == null) return;

        mRootView = new FrameLayout(context) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (mOnKeyListener != null && mOnKeyListener.onKey(event)) {
                    return true;
                }

                // 返回键处理
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (mCancelable && !isDismissing) {
                        dismiss();
                        return true;
                    }
                }
                return super.dispatchKeyEvent(event);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (mBlockBackgroundTouch) {
                    final float x = ev.getX();
                    final float y = ev.getY();
                    mIsTouchInside = !isOutOfContentArea(x, y);
                    return !mIsTouchInside;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (mDraggable && mContentView != null) {
                    handleDragEvent(event);
                    return true;
                }

                if (mCancelableOnTouchOutside && !mIsTouchInside) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!mIsDragging && isOutOfContentArea(event.getX(), event.getY())) {
                            if (mOnTouchOutsideListener != null) {
                                mOnTouchOutsideListener.onTouchOutside();
                            }
                            dismiss();
                            return true;
                        }
                    }
                }
                return true;
            }
        };

        mRootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * 处理拖拽事件
     */
    protected void handleDragEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY() - mStatusBarHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mInitialX = mWindowParams.x;
                mInitialY = mWindowParams.y;
                mIsDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!mIsDragging) {
                    float dx = Math.abs(x - mDownX);
                    float dy = Math.abs(y - mDownY);
                    if (dx > mTouchSlop || dy > mTouchSlop) {
                        mIsDragging = true;
                    }
                } else {
                    int newX = (int) (mInitialX + (x - mDownX));
                    int newY = (int) (mInitialY + (y - mDownY));

                    // 限制拖拽范围，动态计算导航栏高度
                    newX = Math.max(-mScreenWidth / 2, Math.min(newX, mScreenWidth / 2));
                    newY = Math.max(-mScreenHeight / 2 + mStatusBarHeight, Math.min(newY, mScreenHeight / 2 - getNavigationBarHeight()));

                    mWindowParams.x = newX;
                    mWindowParams.y = newY;
                    updateWindowParams();
                }
                break;

            case MotionEvent.ACTION_UP:
                mIsDragging = false;
                break;
        }
    }

    private int getNavigationBarHeight() {
        Context context = getContext();
        if (context == null) return 0;

        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    /**
     * 初始化背景遮罩
     */
    protected void initBackgroundView() {
        Context context = getContext();
        if (context == null || mRootView == null) return;

        mBackgroundView = new View(context);
        mBackgroundView.setBackgroundColor(0x80000000);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mRootView.addView(mBackgroundView, params);
    }

    /**
     * 初始化内容视图
     */
    protected void initContentView() {
        Context context = getContext();
        if (context == null || mRootView == null) return;

        int layoutResId = getLayoutResId();
        if (layoutResId <= 0) {
            Log.e(TAG, "子类必须提供有效的布局资源ID");
            return;
        }

        mContentView = LayoutInflater.from(context).inflate(layoutResId, mRootView, false);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER;
        mRootView.addView(mContentView, contentParams);
    }

    /**
     * 检查触摸点是否在内容区域外
     */
    protected boolean isOutOfContentArea(float x, float y) {
        if (mContentView == null) return true;

        int[] location = new int[2];
        mContentView.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + mContentView.getWidth();
        int bottom = top + mContentView.getHeight();

        return x < left || x > right || y < top || y > bottom;
    }

    /**
     * 显示弹窗
     */
    public void show() {
        show(null);
    }

    /**
     * 显示弹窗并传递参数
     */
    public void show(@Nullable Bundle args) {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "显示失败：Application Context已失效");
            return;
        }

        if (!hasOverlayPermission()) {
            Log.w(TAG, "未获取悬浮窗权限，无法显示弹窗。请在设置中手动开启权限。");
            return;
        }

        if (isShowing || isDismissing || mRootView == null || mWindowManager == null) {
            Log.w(TAG, "显示失败：弹窗状态异常");
            return;
        }

        if (args != null) {
            handleArguments(args);
        }

        try {
            applyEnterAnimation();
            mWindowManager.addView(mRootView, mWindowParams);
            isShowing = true;
            isDismissing = false;

            // 回调显示事件
            mMainHandler.post(() -> {
                if (mOnShowListener != null) {
                    mOnShowListener.onShow();
                }
            });
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.e(TAG, "Android 8.0+ 显示失败，可能是权限未开启或类型错误", e);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(TAG, "Android 6.0-7.1 显示失败，可能是权限未开启", e);
            } else {
                Log.e(TAG, "低版本Android显示失败", e);
            }
            onShowFailed();
        }
    }

    /**
     * 处理传递的参数
     */
    protected void handleArguments(@NonNull Bundle args) {

    }

    /**
     * 显示失败回调
     */
    protected void onShowFailed() {
        isShowing = false;
        isDismissing = false;
    }

    /**
     * 关闭弹窗
     */
    public void dismiss() {
        dismiss(true);
    }

    /**
     * 关闭弹窗
     */
    public void dismiss(boolean withAnimation) {
        cancelAutoDismiss();

        Context context = getContext();
        if (context == null || !isShowing || isDismissing || mRootView == null) {
            return;
        }

        isDismissing = true;

        try {
            if (withAnimation && (hasEnterAnimation() || hasExitAnimation())) {
                applyExitAnimation(this::removeViewAndCleanup);
            } else {
                removeViewAndCleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭弹窗失败", e);
            isDismissing = false;
        }
    }

    /**
     * 移除视图并清理资源
     */
    protected void removeViewAndCleanup() {
        if (mRootView == null || mWindowManager == null) return;

        try {
            if (mRootView.getParent() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mWindowManager.removeViewImmediate(mRootView);
                } else {
                    mWindowManager.removeView(mRootView);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "移除视图失败", e);
        }

        isShowing = false;
        isDismissing = false;

        mMainHandler.post(() -> {
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss();
            }
        });

        onDismissed();
    }

    /**
     * 应用进入动画
     */
    protected void applyEnterAnimation() {
        Context context = getContext();
        if (context == null) return;

        // 内容进入动画
        if (mContentView != null) {
            if (mEnterAnimResId > 0) {
                try {
                    mEnterAnimation = AnimationUtils.loadAnimation(context, mEnterAnimResId);
                    mContentView.startAnimation(mEnterAnimation);
                } catch (Exception e) {
                    Log.w(TAG, "加载进入动画失败，使用默认动画", e);
                    applyDefaultEnterAnimation(mContentView);
                }
            } else {
                applyDefaultEnterAnimation(mContentView);
            }
        }

        // 背景进入动画
        if (mBackgroundView != null) {
            if (mBackgroundEnterAnimResId > 0) {
                try {
                    mBackgroundEnterAnim = AnimationUtils.loadAnimation(context, mBackgroundEnterAnimResId);
                    mBackgroundView.startAnimation(mBackgroundEnterAnim);
                } catch (Exception e) {
                    Log.w(TAG, "加载背景进入动画失败，使用默认动画", e);
                    applyDefaultEnterAnimation(mBackgroundView);
                }
            } else {
                applyDefaultEnterAnimation(mBackgroundView);
            }
        }
    }

    private void applyDefaultEnterAnimation(@NonNull View view) {
        Animation defaultAnimation = new AlphaAnimation(0f, 1f); // 从透明到不透明
        defaultAnimation.setDuration(300); // 动画时长 300ms
        view.startAnimation(defaultAnimation);
    }

    /**
     * 应用退出动画
     */
    protected void applyExitAnimation(final Runnable endAction) {
        Context context = getContext();
        if (context == null || endAction == null) return;

        final int[] animCount = {0};
        final int[] completedCount = {0};

        // 内容退出动画
        if (mContentView != null) {
            if (mExitAnimResId > 0) {
                try {
                    animCount[0]++;
                    mExitAnimation = AnimationUtils.loadAnimation(context, mExitAnimResId);
                    mExitAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (++completedCount[0] >= animCount[0]) {
                                mMainHandler.post(endAction);
                            }
                        }

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    mContentView.startAnimation(mExitAnimation);
                } catch (Exception e) {
                    Log.w(TAG, "加载退出动画失败，使用默认动画", e);
                    applyDefaultExitAnimation(mContentView, endAction);
                }
            } else {
                applyDefaultExitAnimation(mContentView, endAction);
            }
        }

        // 背景退出动画
        if (mBackgroundView != null) {
            if (mBackgroundExitAnimResId > 0) {
                try {
                    animCount[0]++;
                    mBackgroundExitAnim = AnimationUtils.loadAnimation(context, mBackgroundExitAnimResId);
                    mBackgroundExitAnim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (++completedCount[0] >= animCount[0]) {
                                mMainHandler.post(endAction);
                            }
                        }

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    mBackgroundView.startAnimation(mBackgroundExitAnim);
                } catch (Exception e) {
                    Log.w(TAG, "加载背景退出动画失败，使用默认动画", e);
                    applyDefaultExitAnimation(mBackgroundView, endAction);
                }
            } else {
                applyDefaultExitAnimation(mBackgroundView, endAction);
            }
        }

        if (animCount[0] == 0) {
            mMainHandler.post(endAction);
        }
    }

    private void applyDefaultExitAnimation(@NonNull View view, @NonNull Runnable endAction) {
        Animation defaultAnimation = new AlphaAnimation(1f, 0f); // 从不透明到透明
        defaultAnimation.setDuration(300); // 动画时长 300ms
        defaultAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mMainHandler.post(endAction);
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(defaultAnimation);
    }

    /**
     * 弹窗消失后清理资源
     */
    protected void onDismissed() {
        mEnterAnimation = null;
        mExitAnimation = null;
        mBackgroundEnterAnim = null;
        mBackgroundExitAnim = null;
        mMainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 更新窗口参数
     */
    protected void updateWindowParams() {
        if (isShowing && mRootView != null && mWindowManager != null) {
            try {
                mWindowManager.updateViewLayout(mRootView, mWindowParams);
            } catch (Exception e) {
                Log.e(TAG, "更新窗口参数失败", e);
            }
        }
    }

    /**
     * 判断是否有进入动画
     */
    private boolean hasEnterAnimation() {
        return mEnterAnimResId > 0 || mBackgroundEnterAnimResId > 0;
    }

    /**
     * 判断是否有退出动画
     */
    private boolean hasExitAnimation() {
        return mExitAnimResId > 0 || mBackgroundExitAnimResId > 0;
    }

    /**
     * 设置是否可拖拽
     */
    public void setDraggable(boolean draggable) {
        mDraggable = draggable;
    }

    /**
     * 设置弹窗位置
     */
    public void setPosition(int gravity, int x, int y) {
        if (mWindowParams != null) {
            mWindowParams.gravity = gravity;
            mWindowParams.x = x;
            mWindowParams.y = y;
            updateWindowParams();
        }
    }

    /**
     * 设置弹窗大小
     */
    public void setSize(int width, int height) {
        if (mWindowParams != null) {
            mWindowParams.width = width;
            mWindowParams.height = height;
            updateWindowParams();
        }
    }

    /**
     * 设置宽度为屏幕比例
     */
    public void setWidthRatio(float ratio) {
        if (mScreenWidth > 0 && ratio > 0 && ratio <= 1) {
            setSize((int) (mScreenWidth * ratio), mWindowParams.height);
        }
    }

    /**
     * 设置高度为屏幕比例
     */
    public void setHeightRatio(float ratio) {
        if (mScreenHeight > 0 && ratio > 0 && ratio <= 1) {
            setSize(mWindowParams.width, (int) (mScreenHeight * ratio));
        }
    }

    /**
     * 设置背景透明度
     */
    public void setBackgroundAlpha(float alpha) {
        if (mBackgroundView != null) {
            int alphaInt = (int) (Math.max(0, Math.min(alpha, 1)) * 255);
            mBackgroundView.setBackgroundColor((alphaInt << 24) | 0x000000);
        }
    }

    /**
     * 设置自动消失时间
     */
    public void setAutoDismiss(long delayMillis) {
        if (delayMillis <= 0) return;
        cancelAutoDismiss();
        mMainHandler.postDelayed(mAutoDismissRunnable, delayMillis);
    }

    /**
     * 取消自动消失
     */
    public void cancelAutoDismiss() {
        mMainHandler.removeCallbacks(mAutoDismissRunnable);
    }

    /**
     * 设置进入动画
     */
    public void setEnterAnimation(@StyleRes int resId) {
        mEnterAnimResId = resId;
    }

    /**
     * 设置退出动画
     */
    public void setExitAnimation(@StyleRes int resId) {
        mExitAnimResId = resId;
    }

    /**
     * 设置背景进入动画
     */
    public void setBackgroundEnterAnimation(@StyleRes int resId) {
        mBackgroundEnterAnimResId = resId;
    }

    /**
     * 设置背景退出动画
     */
    public void setBackgroundExitAnimation(@StyleRes int resId) {
        mBackgroundExitAnimResId = resId;
    }

    /**
     * 判断是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * 获取内容视图
     */
    @Nullable
    public View getContentView() {
        return mContentView;
    }

    /**
     * 查找子视图
     */
    @Nullable
    public <T extends View> T findViewById(int id) {
        return mContentView != null ? mContentView.findViewById(id) : null;
    }

    /**
     * 设置显示监听器
     */
    public void setOnShowListener(OnShowListener listener) {
        mOnShowListener = listener;
    }

    /**
     * 设置消失监听器
     */
    public void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    /**
     * 设置按键监听器
     */
    public void setOnKeyListener(OnKeyListener listener) {
        mOnKeyListener = listener;
    }

    /**
     * 设置外部触摸监听器
     */
    public void setOnTouchOutsideListener(OnTouchOutsideListener listener) {
        mOnTouchOutsideListener = listener;
    }

    /**
     * 获取布局资源ID
     */
    protected abstract int getLayoutResId();

    /**
     * 初始化视图
     */
    protected void setupViews() {
    }

    /**
     * 设置事件监听
     */
    protected void setupEvents() {
    }

    /**
     * 弹窗显示监听器
     */
    public interface OnShowListener {
        void onShow();
    }

    /**
     * 弹窗消失监听器
     */
    public interface OnDismissListener {
        void onDismiss();
    }

    /**
     * 按键监听器
     */
    public interface OnKeyListener {
        boolean onKey(KeyEvent event);
    }

    /**
     * 外部触摸监听器
     */
    public interface OnTouchOutsideListener {
        void onTouchOutside();
    }
}