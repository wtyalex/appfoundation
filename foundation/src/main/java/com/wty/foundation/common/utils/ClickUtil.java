package com.wty.foundation.common.utils;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;

/**
 * @author wutianyu
 * @createTime 2024/8/21 9:21
 * @describe
 */
public class ClickUtil {

    /**
     * 扩展指定View的点击区域。
     *
     * @param view 要扩展点击区域的View。
     * @param leftPadding 左侧额外的点击区域（单位：像素）。
     * @param topPadding 顶部额外的点击区域（单位：像素）。
     * @param rightPadding 右侧额外的点击区域（单位：像素）。
     * @param bottomPadding 底部额外的点击区域（单位：像素）。
     */
    public static void expandTouchArea(final View view, final int leftPadding, final int topPadding,
        final int rightPadding, final int bottomPadding) {
        final Rect delegateArea = new Rect();

        // 使用post方法确保在UI线程中执行，且在视图已经绘制完成之后执行
        view.post(new Runnable() {
            @Override
            public void run() {
                // 获取当前View的边界
                view.getHitRect(delegateArea);

                // 扩展边界
                delegateArea.left -= leftPadding;
                delegateArea.top -= topPadding;
                delegateArea.right += rightPadding;
                delegateArea.bottom += bottomPadding;

                // 设置TouchDelegate
                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    final View parentView = (View)parent;
                    parentView.post(new Runnable() {
                        @Override
                        public void run() {
                            TouchDelegate touchDelegate = new TouchDelegate(delegateArea, view);
                            parentView.setTouchDelegate(touchDelegate);
                        }
                    });
                }
            }
        });
    }

}