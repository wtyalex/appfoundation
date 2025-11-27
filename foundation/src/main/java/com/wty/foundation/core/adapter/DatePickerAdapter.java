package com.wty.foundation.core.adapter;

import com.wty.foundation.databinding.UiAdapterPickerItemBinding;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

/**
 * @author wutianyu
 * @createTime 2023/2/9 13:03
 * @describe 日期选择器适配器，用于处理日期选择列表的显示和文本转换
 */
public abstract class DatePickerAdapter extends BaseAdapter<DatePickerAdapter.InnerViewHolder, String> {
    @NonNull
    @Override
    public InnerViewHolder onCreateViewHolder1(@NonNull ViewGroup parent, int viewType) {
        return new InnerViewHolder(
            UiAdapterPickerItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    class InnerViewHolder extends BaseViewHolder<UiAdapterPickerItemBinding, String> {

        public InnerViewHolder(@NonNull UiAdapterPickerItemBinding adapterPickerBinding) {
            super(adapterPickerBinding);
        }

        @Override
        protected void dealData() {
            mVB.content.setText(OnText(getAdapterPosition(), ""));
        }
    }

    protected abstract String OnText(int position, String data);
}
