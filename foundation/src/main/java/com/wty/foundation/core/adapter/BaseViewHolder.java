package com.wty.foundation.core.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

/**
 * @author wutianyu
 * @createTime 2023/1/31 9:23
 * @describe RecyclerView的ViewHolder基类，负责数据绑定和点击事件处理
 */
public abstract class BaseViewHolder<VB extends ViewBinding, D> extends RecyclerView.ViewHolder {
    protected @NonNull final VB mVB;
    protected @Nullable D mData;
    protected @Nullable OnClickListener<D> mOnClickListener;

    public BaseViewHolder(@NonNull VB vb) {
        super(vb.getRoot());
        mVB = vb;
        itemView.setOnClickListener(v -> {
            if (mOnClickListener != null) {
                mOnClickListener.onItemClick(mData, getAdapterPosition(), itemView);
            }
        });
    }

    final void setData(@Nullable D data) {
        mData = data;
        dealData();
    }

    final void setOnClickListener(@Nullable OnClickListener<D> onClickListener) {
        mOnClickListener = onClickListener;
    }

    /**
     * 把数据绑定到视图，并作一些逻辑处理
     */
    protected abstract void dealData();
}
