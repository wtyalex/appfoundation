package com.wty.foundation.core.adapter;

import android.view.View;

import androidx.annotation.Nullable;

/**
 * @param <D> 实体数据类
 * @author wutianyu
 * @createTime 2023/1/31 9:23
 * @describe RecyclerView.Adapter 点击事件封装类
 */
public interface OnClickListener<D> {
    /**
     * ItemView 被点击
     * 
     * @param data 被点击条目的数据
     * @param position 被点击条目在Adapter中的位置
     * @param itemView 被点击条目View
     */
    default void onItemClick(@Nullable D data, int position, @Nullable View itemView) {}

    /**
     * ItemView中的子View被点击
     * 
     * @param data 被点击子View的数据
     * @param position 被点击子View在Adapter中的位置
     * @param childView 被点击子ViewView
     */
    default void onItemChildViewClick(@Nullable D data, int position, @Nullable View childView) {}
}