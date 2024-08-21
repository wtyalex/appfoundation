package com.wty.foundation.core.recycleview;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DefaultItemDecoration extends RecyclerView.ItemDecoration {
    private int spacing;
    private int startSpacing;
    private int endSpacing;

    public DefaultItemDecoration(int spacing) {
        this(spacing, 0);
    }

    public DefaultItemDecoration(int spacing, int bothSpacing) {
        this(spacing, bothSpacing, bothSpacing);
    }

    public DefaultItemDecoration(int spacing, int startSpacing, int endSpacing) {
        this.spacing = spacing;
        this.startSpacing = startSpacing;
        this.endSpacing = endSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state) {
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            if (((LinearLayoutManager)lm).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.left = startSpacing;
                } else {
                    outRect.left = spacing / 2;
                }
                if (parent.getChildAdapterPosition(view) == state.getItemCount() - 1) {
                    outRect.right = endSpacing;
                } else {
                    outRect.right = spacing / 2;
                }
            } else {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = startSpacing;
                } else {
                    outRect.top = spacing / 2;
                }
                if (parent.getChildAdapterPosition(view) == state.getItemCount() - 1) {
                    outRect.bottom = endSpacing;
                } else {
                    outRect.bottom = spacing / 2;
                }
            }
        }
    }
}
