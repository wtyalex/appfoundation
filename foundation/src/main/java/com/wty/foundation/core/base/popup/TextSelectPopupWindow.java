package com.wty.foundation.core.base.popup;

import java.util.List;

import com.wty.foundation.common.utils.ArrayUtils;
import com.wty.foundation.core.adapter.BaseAdapter;
import com.wty.foundation.core.adapter.BaseViewHolder;
import com.wty.foundation.core.adapter.OnClickListener;
import com.wty.foundation.databinding.UiAdapterTextBinding;
import com.wty.foundation.databinding.UiPopupTextSelectBinding;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * @author wutianyu
 * @createTime 2023/2/8 14:01
 * @describe
 */
public abstract class TextSelectPopupWindow<D> extends PopupWindow {
    private final UiPopupTextSelectBinding mViewBinding;
    private final Adapter mAdapter;
    private OnItemSelectedListener<D> onItemSelectedListener;

    public TextSelectPopupWindow(Context context) {
        super();
        mAdapter = new Adapter();
        mAdapter.setOnClickListener(new OnClickListener<D>() {
            @Override
            public void onItemClick(D data, int position, View itemView) {
                if (onItemSelectedListener != null) {
                    onItemSelectedListener.onItemSelected(data);
                }
            }
        });
        mViewBinding = UiPopupTextSelectBinding.inflate(LayoutInflater.from(context), null, false);
        initView();
    }

    public void setDatas(List<D> datas) {
        mAdapter.setDatas(datas);
        mAdapter.notifyDataSetChanged();
    }

    private void initView() {
        setContentView(mViewBinding.getRoot());
        mViewBinding.list.setLayoutManager(new LinearLayoutManager(mViewBinding.getRoot().getContext()));
        mViewBinding.list.setAdapter(mAdapter);
        setBackgroundDrawable(new ColorDrawable(0x00000000));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);
        setFocusable(true);
        setTouchable(true);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener<D> onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener<D> {
        void onItemSelected(D data);
    }

    private class Adapter extends BaseAdapter<Adapter.InnerViewHolder, D> {

        @NonNull
        @Override
        public InnerViewHolder onCreateViewHolder1(@NonNull ViewGroup parent, int viewType) {
            return new InnerViewHolder(
                UiAdapterTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        private class InnerViewHolder extends BaseViewHolder<UiAdapterTextBinding, D> {

            public InnerViewHolder(@NonNull UiAdapterTextBinding adapterTextBinding) {
                super(adapterTextBinding);
            }

            @Override
            protected void dealData() {
                mVB.name.setText(onText(mData));
                int position = getAdapterPosition();
                if (position == ArrayUtils.size(mDatas) - 1) {
                    mVB.line.setVisibility(View.GONE);
                } else {
                    mVB.line.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    protected abstract String onText(D data);

}
