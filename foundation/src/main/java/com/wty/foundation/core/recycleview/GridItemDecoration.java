package com.wty.foundation.core.recycleview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author wutianyu
 * @createTime 2023/2/21 10:12
 * @describe 网格布局RecyclerView的Item装饰类，用于设置item间距
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private int verticalSpace = 0;
    private int horizontalSpace = 0;

    public GridItemDecoration(int verticalSpace, int horizontalSpace) {
        this.verticalSpace = verticalSpace;
        this.horizontalSpace = horizontalSpace;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state) {
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        if (!(lm instanceof GridLayoutManager)) {
            return;
        }
        GridLayoutManager glm = (GridLayoutManager)lm;
        int position = parent.getChildAdapterPosition(view);
        int spanCount = glm.getSpanCount();
        float r0 = (spanCount * horizontalSpace * 1f) / (spanCount + 1);
        float adjust = ((horizontalSpace - r0) / spanCount);
        r0 = r0 - adjust;
        Log.d("GridItemDecoration", "r0:" + r0);
        if (position < spanCount) {
            outRect.top = 0;
        } else {
            outRect.top = verticalSpace;
        }
        outRect.left = (int)((horizontalSpace - r0) * (position % spanCount));
        outRect.right = (int)r0 - outRect.left;
        Log.d("GridItemDecoration", position + " :outRect: " + outRect);
    }
}
