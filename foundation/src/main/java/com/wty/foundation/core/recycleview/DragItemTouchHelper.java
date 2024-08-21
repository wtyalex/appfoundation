package com.wty.foundation.core.recycleview;

import com.wty.foundation.R;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class DragItemTouchHelper extends ItemTouchHelper.Callback {
    private int mFromPosition = -1;
    private int mToPosition = -1;
    private OnDragListener mOnDragListener;

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
        @NonNull RecyclerView.ViewHolder target) {
        recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
        // 得到当拖拽的viewHolder的Position
        int fromPosition = viewHolder.getAdapterPosition();
        if (mFromPosition == -1) {
            mFromPosition = fromPosition;
        }
        // 拿到当前拖拽到的item的viewHolder
        mToPosition = target.getAdapterPosition();
        recyclerView.getAdapter().notifyItemMoved(fromPosition, mToPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

    /**
     * 长按选中Item时修改颜色
     *
     * @param viewHolder
     * @param actionState
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewHolder.itemView
                    .setForeground(viewHolder.itemView.getContext().getDrawable(R.drawable.shape_light_blue_6_radius));
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    /**
     * 手指松开的时候还原颜色
     *
     * @param recyclerView
     * @param viewHolder
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewHolder.itemView.setForeground(null);
        }
        if (mOnDragListener != null && mFromPosition != mToPosition) {
            mOnDragListener.onMoved(mFromPosition, mToPosition);
        }
        mFromPosition = -1;
        mToPosition = -1;
    }

    /**
     * 重写拖拽不可用
     *
     * @return
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        this.mOnDragListener = onDragListener;
    }

    public interface OnDragListener {
        void onMoved(int fromPosition, int toPosition);
    }
}
