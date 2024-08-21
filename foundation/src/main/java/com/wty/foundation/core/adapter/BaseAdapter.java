package com.wty.foundation.core.adapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.wty.foundation.common.utils.ArrayUtils;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @param <VH> 继承BaseViewHolder的ViewHolder
 * @param <D> 实体数据类
 * @author wutianyu
 * @createTime 2023/1/31 9:23
 * @describe RecyclerView.Adapter 封装类
 */
public abstract class BaseAdapter<VH extends BaseViewHolder<?, D>, D> extends RecyclerView.Adapter<VH>
    implements OnClickListener<D> {
    protected @NonNull final ArrayList<D> mDatas = new ArrayList<>();
    private @NonNull OnClickListener<D> mOnClickListener;

    public BaseAdapter() {
        registerAdapterDataObserver(new AdapterDataChangeObserver(this));
    }

    @NonNull
    @Override
    public final VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VH vieHolder = onCreateViewHolder1(parent, viewType);
        vieHolder.setOnClickListener(this);
        return vieHolder;
    }

    /**
     * 创建ViewHolder
     * 
     * @param parent ViewGroup
     * @param viewType 视图类型
     * @return VH
     */
    @NonNull
    public abstract VH onCreateViewHolder1(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.setData(ArrayUtils.get(mDatas, position));
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public final void onItemChildViewClick(D data, int position, View childView) {
        OnClickListener<D> listener = mOnClickListener;
        if (listener != null) {
            listener.onItemChildViewClick(data, position, childView);
        }
    }

    @Override
    public final void onItemClick(D data, int position, View itemView) {
        OnClickListener<D> listener = mOnClickListener;
        if (listener != null) {
            listener.onItemClick(data, position, itemView);
        }
    }

    /**
     * 清空后设置数据
     * 
     * @param datas 数据
     */
    public void setDatas(@Nullable List<D> datas) {
        mDatas.clear();
        if (!ArrayUtils.isEmpty(datas)) {
            mDatas.addAll(datas);
        }
    }

    /**
     * 在指定位置上插入多条数据
     * 
     * @param startPosition
     * @param datas
     */
    public final void notifyItemRangeInserted(int startPosition, List<D> datas) {
        if (ArrayUtils.isEmpty(datas)) {
            return;
        }
        mDatas.addAll(startPosition, datas);
        notifyItemRangeInserted(startPosition, datas.size());
    }

    /**
     * 在指定位置上插入一条数据
     * 
     * @param position
     * @param data
     */
    public final void notifyItemInserted(int position, D data) {
        mDatas.add(position, data);
        notifyItemInserted(position);
    }

    /**
     * 获取指定位置的数据
     * 
     * @param position
     * @return
     */
    public D getDataByPosition(int position) {
        return ArrayUtils.get(mDatas, position);
    }

    /**
     * 追加数据
     * 
     * @param datas 数据
     */
    public void addData(@NonNull List<D> datas) {
        if (!ArrayUtils.isEmpty(datas)) {
            mDatas.addAll(datas);
        }
    }

    /**
     * 设置点击事件监听
     * 
     * @param onClickListener OnClickListener
     */
    public void setOnClickListener(@NonNull OnClickListener<D> onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    static class AdapterDataChangeObserver extends RecyclerView.AdapterDataObserver {
        private final WeakReference<BaseAdapter> mAdapter;

        public AdapterDataChangeObserver(BaseAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            BaseAdapter adapter = mAdapter.get();
            if (adapter == null) {
                return;
            }
            if (fromPosition >= 0 && fromPosition < adapter.mDatas.size() && toPosition >= 0
                && toPosition < adapter.mDatas.size()) {
                adapter.mDatas.add(toPosition, adapter.mDatas.remove(fromPosition));
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            BaseAdapter adapter = mAdapter.get();
            if (adapter == null) {
                return;
            }
            int toPosition = positionStart + itemCount;
            for (int i = adapter.mDatas.size() - 1; i >= 0; i--) {
                if (i >= positionStart && i < toPosition) {
                    adapter.mDatas.remove(i);
                }
            }
        }
    }
}
