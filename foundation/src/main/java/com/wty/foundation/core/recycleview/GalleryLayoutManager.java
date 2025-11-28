package com.wty.foundation.core.recycleview;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.wty.foundation.BuildConfig;

/**
 * 画廊布局管理器，用于实现类似ViewPager的轮播效果，支持水平和垂直方向滚动
 */
public class GalleryLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    private static final String TAG = "GalleryLayoutManager";
    final static int LAYOUT_START = -1;

    final static int LAYOUT_END = 1;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    private int mFirstVisiblePosition = 0;
    private int mLastVisiblePos = 0;
    private int mInitialSelectedPosition = 0;

    int mCurSelectedPosition = -1;

    View mCurSelectedView;
    /**
     * 滚动状态
     */
    private State mState;

    private LinearSnapHelper mSnapHelper = new LinearSnapHelper();

    private InnerScrollListener mInnerScrollListener = new InnerScrollListener();

    private boolean mCallbackInFling = false;

    /**
     * 当前方向，水平或垂直
     */
    private int mOrientation = HORIZONTAL;

    private OrientationHelper mHorizontalHelper;
    private OrientationHelper mVerticalHelper;

    public GalleryLayoutManager(int orientation) {
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public int getCurSelectedPosition() {
        return mCurSelectedPosition;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == VERTICAL) {
            return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLayoutChildren() 调用，state = [" + state + "]");
        }
        if (getItemCount() == 0) {
            reset();
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        if (state.getItemCount() != 0 && !state.didStructureChange()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLayoutChildren: 忽略额外的布局步骤");
            }
            return;
        }
        if (getChildCount() == 0 || state.didStructureChange()) {
            reset();
        }
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        detachAndScrapAttachedViews(recycler);
        firstFillCover(recycler, state, 0);
    }

    private void reset() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "reset: ");
        }
        if (mState != null) {
            mState.mItemsFrames.clear();
        }
        // 当数据集更新时保持最后选中的位置
        if (mCurSelectedPosition != -1) {
            mInitialSelectedPosition = mCurSelectedPosition;
        }
        mInitialSelectedPosition = Math.min(Math.max(0, mInitialSelectedPosition), getItemCount() - 1);
        mFirstVisiblePosition = mInitialSelectedPosition;
        mLastVisiblePos = mInitialSelectedPosition;
        mCurSelectedPosition = -1;
        if (mCurSelectedView != null) {
            mCurSelectedView.setSelected(false);
            mCurSelectedView = null;
        }
    }

    private void firstFillCover(RecyclerView.Recycler recycler, RecyclerView.State state, int scrollDelta) {
        if (mOrientation == HORIZONTAL) {
            firstFillWithHorizontal(recycler, state);
        } else {
            firstFillWithVertical(recycler, state);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "firstFillCover 完成:first: " + mFirstVisiblePosition + ",last:" + mLastVisiblePos);
        }

        if (mItemTransformer != null) {
            View child;
            for (int i = 0; i < getChildCount(); i++) {
                child = getChildAt(i);
                mItemTransformer.transformItem(this, child, calculateToCenterFraction(child, scrollDelta));
            }
        }
        mInnerScrollListener.onScrolled(mRecyclerView, 0, 0);
    }

    /**
     * 首先布局由 {@link GalleryLayoutManager#mInitialSelectedPosition} 指定位置的项视图，
     * 然后布局其他的视图
     *
     * @param recycler 回收器
     * @param state    状态
     */
    private void firstFillWithHorizontal(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        int startPosition = mInitialSelectedPosition;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        int topOffset;
        // 布局初始位置视图
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        addView(scrap, 0);
        measureChildWithMargins(scrap, 0, 0);
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
        int left = (int) (getPaddingLeft() + (getHorizontalSpace() - scrapWidth) / 2.f);
        scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight);
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        int leftStartOffset = getDecoratedLeft(scrap);
        int rightStartOffset = getDecoratedRight(scrap);
        // 填充中心左侧
        fillLeft(recycler, mInitialSelectedPosition - 1, leftStartOffset, leftEdge);
        // 填充中心右侧
        fillRight(recycler, mInitialSelectedPosition + 1, rightStartOffset, rightEdge);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    /**
     * 首先布局由 {@link GalleryLayoutManager#mInitialSelectedPosition} 指定位置的项视图，
     * 然后布局其他的视图
     *
     * @param recycler 回收器
     * @param state    状态
     */
    private void firstFillWithVertical(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();
        int startPosition = mInitialSelectedPosition;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        int leftOffset;
        // 布局初始位置视图
        View scrap = recycler.getViewForPosition(mInitialSelectedPosition);
        addView(scrap, 0);
        measureChildWithMargins(scrap, 0, 0);
        scrapWidth = getDecoratedMeasuredWidth(scrap);
        scrapHeight = getDecoratedMeasuredHeight(scrap);
        leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
        int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
        scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
        layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
        if (getState().mItemsFrames.get(startPosition) == null) {
            getState().mItemsFrames.put(startPosition, scrapRect);
        } else {
            getState().mItemsFrames.get(startPosition).set(scrapRect);
        }
        mFirstVisiblePosition = mLastVisiblePos = startPosition;
        int topStartOffset = getDecoratedTop(scrap);
        int bottomStartOffset = getDecoratedBottom(scrap);
        // 填充中心上方
        fillTop(recycler, mInitialSelectedPosition - 1, topStartOffset, topEdge);
        // 填充中心下方
        fillBottom(recycler, mInitialSelectedPosition + 1, bottomStartOffset, bottomEdge);
    }

    /**
     * 填充中心视图的左侧
     *
     * @param recycler      回收器
     * @param startPosition 开始填充的起始位置
     * @param startOffset   布局起始偏移量
     * @param leftEdge      左边缘
     */
    private void fillLeft(RecyclerView.Recycler recycler, int startPosition, int startOffset, int leftEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i >= 0 && startOffset > leftEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
            scrapRect.set(startOffset - scrapWidth, topOffset, startOffset, topOffset + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.left;
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 填充中心视图的右侧
     *
     * @param recycler      回收器
     * @param startPosition 开始填充的起始位置
     * @param startOffset   布局起始偏移量
     * @param rightEdge     右边缘
     */
    private void fillRight(RecyclerView.Recycler recycler, int startPosition, int startOffset, int rightEdge) {
        View scrap;
        int topOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int height = getVerticalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < rightEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
            scrapRect.set(startOffset, topOffset, startOffset + scrapWidth, topOffset + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.right;
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 填充中心视图的上方
     *
     * @param recycler      回收器
     * @param startPosition 开始填充的起始位置
     * @param startOffset   布局起始偏移量
     * @param topEdge       RecycleView的顶部边缘
     */
    private void fillTop(RecyclerView.Recycler recycler, int startPosition, int startOffset, int topEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i >= 0 && startOffset > topEdge; i--) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap, 0);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.top;
            mFirstVisiblePosition = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    /**
     * 填充中心视图的下方
     *
     * @param recycler      回收器
     * @param startPosition 开始填充的起始位置
     * @param startOffset   布局起始偏移量
     * @param bottomEdge    RecycleView的底部边缘
     */
    private void fillBottom(RecyclerView.Recycler recycler, int startPosition, int startOffset, int bottomEdge) {
        View scrap;
        int leftOffset;
        int scrapWidth, scrapHeight;
        Rect scrapRect = new Rect();
        int width = getHorizontalSpace();
        for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge; i++) {
            scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);
            scrapWidth = getDecoratedMeasuredWidth(scrap);
            scrapHeight = getDecoratedMeasuredHeight(scrap);
            leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
            scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
            layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
            startOffset = scrapRect.bottom;
            mLastVisiblePos = i;
            if (getState().mItemsFrames.get(i) == null) {
                getState().mItemsFrames.put(i, scrapRect);
            } else {
                getState().mItemsFrames.get(i).set(scrapRect);
            }
        }
    }

    private void fillCover(RecyclerView.Recycler recycler, RecyclerView.State state, int scrollDelta) {
        if (getItemCount() == 0) {
            return;
        }

        if (mOrientation == HORIZONTAL) {
            fillWithHorizontal(recycler, state, scrollDelta);
        } else {
            fillWithVertical(recycler, state, scrollDelta);
        }

        if (mItemTransformer != null) {
            View child;
            for (int i = 0; i < getChildCount(); i++) {
                child = getChildAt(i);
                mItemTransformer.transformItem(this, child, calculateToCenterFraction(child, scrollDelta));
            }
        }
    }

    private float calculateToCenterFraction(View child, float pendingOffset) {
        OrientationHelper orientationHelper = getOrientationHelper();
        int parentCenter = (orientationHelper.getEndAfterPadding() - orientationHelper.getStartAfterPadding()) / 2 + orientationHelper.getStartAfterPadding();

        int distance = calculateDistanceCenter(child, pendingOffset, parentCenter);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "calculateToCenterFraction: distance:" + distance + ",parentCenter:" + parentCenter);
        }

        return Math.max(-1f, Math.min(1f, distance * 1.f / parentCenter));
    }

    /**
     * @param child         子视图
     * @param pendingOffset 子视图将滚动的距离
     * @return 距离中心的距离
     */
    private int calculateDistanceCenter(View child, float pendingOffset, int parentCenter) {
        if (mOrientation == GalleryLayoutManager.HORIZONTAL) {
            return (int) (child.getWidth() / 2 - pendingOffset + child.getLeft() - parentCenter);
        } else {
            return (int) (child.getHeight() / 2 - pendingOffset + child.getTop() - parentCenter);
        }
    }

    /**
     * @param recycler 回收器
     * @param state    状态
     * @param dy       垂直滚动距离
     */
    private void fillWithVertical(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "fillWithVertical: dy:" + dy);
        }
        int topEdge = getOrientationHelper().getStartAfterPadding();
        int bottomEdge = getOrientationHelper().getEndAfterPadding();

        // 1.移除并回收屏幕上消失的视图
        View child;
        if (getChildCount() > 0) {
            if (dy >= 0) {
                // 移除并回收顶部屏幕外的视图
                int fixIndex = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    child = getChildAt(i + fixIndex);
                    if (getDecoratedBottom(child) - dy < topEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mFirstVisiblePosition++;
                        fixIndex--;
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "fillWithVertical: break:" + getPosition(child) + ",bottom:" + getDecoratedBottom(child));
                        }
                        break;
                    }
                }
            } else { // dy<0
                // 移除并回收底部屏幕外的视图
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    child = getChildAt(i);
                    if (getDecoratedTop(child) - dy > bottomEdge) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithVertical: removeAndRecycleView:" + getPosition(child));
                        }
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos--;
                    } else {
                        break;
                    }
                }
            }

        }
        int startPosition = mFirstVisiblePosition;
        int startOffset = -1;
        int scrapWidth, scrapHeight;
        Rect scrapRect;
        int width = getHorizontalSpace();
        int leftOffset;
        View scrap;
        // 2.添加或重新附加项视图以填充屏幕
        if (dy >= 0) {
            if (getChildCount() != 0) {
                View lastView = getChildAt(getChildCount() - 1);
                startPosition = getPosition(lastView) + 1;
                startOffset = getDecoratedBottom(lastView);
            }
            for (int i = startPosition; i < getItemCount() && startOffset < bottomEdge + dy; i++) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                if (startOffset == -1 && startPosition == 0) {
                    // 将第一个位置的项居中布局
                    int top = (int) (getPaddingTop() + (getVerticalSpace() - scrapHeight) / 2.f);
                    scrapRect.set(leftOffset, top, leftOffset + scrapWidth, top + scrapHeight);
                } else {
                    scrapRect.set(leftOffset, startOffset, leftOffset + scrapWidth, startOffset + scrapHeight);
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.bottom;
                mLastVisiblePos = i;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithVertical: add view:" + i + ",startOffset:" + startOffset + ",mLastVisiblePos:" + mLastVisiblePos + ",bottomEdge" + bottomEdge);
                }
            }
        } else {
            // dy<0
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPosition = getPosition(firstView) - 1; // 前一个View的position
                startOffset = getDecoratedTop(firstView);
            }
            for (int i = startPosition; i >= 0 && startOffset > topEdge + dy; i--) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap, 0);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                leftOffset = (int) (getPaddingLeft() + (width - scrapWidth) / 2.0f);
                scrapRect.set(leftOffset, startOffset - scrapHeight, leftOffset + scrapWidth, startOffset);
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.top;
                mFirstVisiblePosition = i;
            }
        }
    }

    /**
     * @param recycler 回收器
     * @param state    状态
     * @param dx       水平滚动距离
     */
    private void fillWithHorizontal(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        int leftEdge = getOrientationHelper().getStartAfterPadding();
        int rightEdge = getOrientationHelper().getEndAfterPadding();
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "fillWithHorizontal() 调用，dx = [" + dx + "],leftEdge:" + leftEdge + ",rightEdge:" + rightEdge);
        }
        // 1.移除并回收屏幕上消失的视图
        View child;
        if (getChildCount() > 0) {
            if (dx >= 0) {
                // 移除并回收左侧屏幕外的视图
                int fixIndex = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    child = getChildAt(i + fixIndex);
                    if (getDecoratedRight(child) - dx < leftEdge) {
                        removeAndRecycleView(child, recycler);
                        mFirstVisiblePosition++;
                        fixIndex--;
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + " mFirstVisiblePosition change to:" + mFirstVisiblePosition);
                        }
                    } else {
                        break;
                    }
                }
            } else { // dx<0
                // 移除并回收右侧屏幕外的视图
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    child = getChildAt(i);
                    if (getDecoratedLeft(child) - dx > rightEdge) {
                        removeAndRecycleView(child, recycler);
                        mLastVisiblePos--;
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "fillWithHorizontal:removeAndRecycleView:" + getPosition(child) + "mLastVisiblePos change to:" + mLastVisiblePos);
                        }
                    }
                }
            }

        }
        // 2.添加或重新附加项视图以填充屏幕
        int startPosition = mFirstVisiblePosition;
        int startOffset = -1;
        int scrapWidth, scrapHeight;
        Rect scrapRect;
        int height = getVerticalSpace();
        int topOffset;
        View scrap;
        if (dx >= 0) {
            if (getChildCount() != 0) {
                View lastView = getChildAt(getChildCount() - 1);
                startPosition = getPosition(lastView) + 1; // 从下一个位置开始布局
                startOffset = getDecoratedRight(lastView);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:向右 startPosition:" + startPosition + ",startOffset:" + startOffset + ",rightEdge:" + rightEdge);
                }
            }
            for (int i = startPosition; i < getItemCount() && startOffset < rightEdge + dx; i++) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
                if (startOffset == -1 && startPosition == 0) {
                    // 将第一个位置的项居中布局
                    int left = (int) (getPaddingLeft() + (getHorizontalSpace() - scrapWidth) / 2.f);
                    scrapRect.set(left, topOffset, left + scrapWidth, topOffset + scrapHeight);
                } else {
                    scrapRect.set(startOffset, topOffset, startOffset + scrapWidth, topOffset + scrapHeight);
                }
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.right;
                mLastVisiblePos = i;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal,layout:mLastVisiblePos: " + mLastVisiblePos);
                }
            }
        } else {
            // dx<0
            if (getChildCount() > 0) {
                View firstView = getChildAt(0);
                startPosition = getPosition(firstView) - 1; // 从上一个位置开始布局
                startOffset = getDecoratedLeft(firstView);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "fillWithHorizontal:向左 startPosition:" + startPosition + ",startOffset:" + startOffset + ",leftEdge:" + leftEdge + ",child count:" + getChildCount());
                }
            }
            for (int i = startPosition; i >= 0 && startOffset > leftEdge + dx; i--) {
                scrapRect = getState().mItemsFrames.get(i);
                scrap = recycler.getViewForPosition(i);
                addView(scrap, 0);
                if (scrapRect == null) {
                    scrapRect = new Rect();
                    getState().mItemsFrames.put(i, scrapRect);
                }
                measureChildWithMargins(scrap, 0, 0);
                scrapWidth = getDecoratedMeasuredWidth(scrap);
                scrapHeight = getDecoratedMeasuredHeight(scrap);
                topOffset = (int) (getPaddingTop() + (height - scrapHeight) / 2.0f);
                scrapRect.set(startOffset - scrapWidth, topOffset, startOffset, topOffset + scrapHeight);
                layoutDecorated(scrap, scrapRect.left, scrapRect.top, scrapRect.right, scrapRect.bottom);
                startOffset = scrapRect.left;
                mFirstVisiblePosition = i;
            }
        }
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public State getState() {
        if (mState == null) {
            mState = new State();
        }
        return mState;
    }

    private int calculateScrollDirectionForPosition(int position) {
        if (getChildCount() == 0) {
            return LAYOUT_START;
        }
        final int firstChildPos = mFirstVisiblePosition;
        return position < firstChildPos ? LAYOUT_START : LAYOUT_END;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        final int direction = calculateScrollDirectionForPosition(targetPosition);
        PointF outVector = new PointF();
        if (direction == 0) {
            return null;
        }
        if (mOrientation == HORIZONTAL) {
            outVector.x = direction;
            outVector.y = 0;
        } else {
            outVector.x = 0;
            outVector.y = direction;
        }
        return outVector;
    }

    /**
     * 状态类
     */
    class State {
        /**
         * 记录所有项目视图在上次布局后的最后位置
         */
        SparseArray<Rect> mItemsFrames;

        /**
         * RecycleView自第一次布局以来的当前滚动距离
         */
        int mScrollDelta;

        public State() {
            mItemsFrames = new SparseArray<Rect>();
            mScrollDelta = 0;
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 当dx为正时，手指从右向左滑动(←)，scrollX+
        if (getChildCount() == 0 || dx == 0) {
            return 0;
        }
        int delta = -dx;
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        View child;
        if (dx > 0) {
            // 如果已到达最后一项，则强制限制
            if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {
                child = getChildAt(getChildCount() - 1);
                delta = -Math.max(0, Math.min(dx, (child.getRight() - child.getLeft()) / 2 + child.getLeft() - parentCenter));
            }
        } else {
            // 如果已到达第一项，则强制限制
            if (mFirstVisiblePosition == 0) {
                child = getChildAt(0);
                delta = -Math.min(0, Math.max(dx, ((child.getRight() - child.getLeft()) / 2 + child.getLeft()) - parentCenter));
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollHorizontallyBy: dx:" + dx + ",fixed:" + delta);
        }
        getState().mScrollDelta = -delta;
        fillCover(recycler, state, -delta);
        offsetChildrenHorizontal(delta);
        return -delta;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        int delta = -dy;
        int parentCenter = (getOrientationHelper().getEndAfterPadding() - getOrientationHelper().getStartAfterPadding()) / 2 + getOrientationHelper().getStartAfterPadding();
        View child;
        if (dy > 0) {
            // 如果已到达最后一项，则强制限制
            if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {
                child = getChildAt(getChildCount() - 1);
                delta = -Math.max(0, Math.min(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        } else {
            // 如果已到达第一项，则强制限制
            if (mFirstVisiblePosition == 0) {
                child = getChildAt(0);
                delta = -Math.min(0, Math.max(dy, (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 + getDecoratedTop(child) - parentCenter));
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "scrollVerticallyBy: dy:" + dy + ",fixed:" + delta);
        }
        getState().mScrollDelta = -delta;
        fillCover(recycler, state, -delta);
        offsetChildrenVertical(delta);
        return -delta;
    }

    public OrientationHelper getOrientationHelper() {
        if (mOrientation == HORIZONTAL) {
            if (mHorizontalHelper == null) {
                mHorizontalHelper = OrientationHelper.createHorizontalHelper(this);
            }
            return mHorizontalHelper;
        } else {
            if (mVerticalHelper == null) {
                mVerticalHelper = OrientationHelper.createVerticalHelper(this);
            }
            return mVerticalHelper;
        }
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }

    private ItemTransformer mItemTransformer;

    public void setItemTransformer(ItemTransformer itemTransformer) {
        mItemTransformer = itemTransformer;
    }

    /**
     * 当附加的项被滚动时，会调用ItemTransformer。这为应用程序提供了使用动画属性将自定义变换应用于项视图的机会。
     */
    public interface ItemTransformer {

        /**
         * 将属性转换应用于给定项。
         *
         * @param layoutManager 当前的LayoutManager
         * @param item          将转换应用于该项
         * @param fraction      相对于当前前端和中心位置的页面分数。0是前端和中心。1是向右一个完整页面位置，-1是向左一个页面位置。
         */
        void transformItem(GalleryLayoutManager layoutManager, View item, float fraction);
    }

    /**
     * 监听选中项的变化
     */
    public interface OnItemSelectedListener {
        /**
         * @param recyclerView 项视图所属的RecyclerView。
         * @param item         当前选中的视图
         * @param position     当前选中视图的位置
         */
        void onItemSelected(RecyclerView recyclerView, View item, int position);
    }

    private OnItemSelectedListener mOnItemSelectedListener;

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener;
    }

    public void attach(RecyclerView recyclerView) {
        this.attach(recyclerView, -1);
    }

    /**
     * @param recyclerView     RecyclerView
     * @param selectedPosition 选中的位置
     */
    public void attach(RecyclerView recyclerView, int selectedPosition) {
        if (recyclerView == null) {
            throw new IllegalArgumentException("附加的RecycleView不能为空!!");
        }
        mRecyclerView = recyclerView;
        mInitialSelectedPosition = Math.max(0, selectedPosition);
        recyclerView.setLayoutManager(this);
        mSnapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(mInnerScrollListener);
    }

    RecyclerView mRecyclerView;

    public void setCallbackInFling(boolean callbackInFling) {
        mCallbackInFling = callbackInFling;
    }

    /**
     * 内部监听器，用于监听选中项的变化
     */
    private class InnerScrollListener extends RecyclerView.OnScrollListener {
        int mState;
        boolean mCallbackOnIdle;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            View snap = mSnapHelper.findSnapView(recyclerView.getLayoutManager());
            if (snap != null) {
                int selectedPosition = recyclerView.getLayoutManager().getPosition(snap);
                if (selectedPosition != mCurSelectedPosition) {
                    if (mCurSelectedView != null) {
                        mCurSelectedView.setSelected(false);
                    }
                    mCurSelectedView = snap;
                    mCurSelectedView.setSelected(true);
                    mCurSelectedPosition = selectedPosition;
                    if (!mCallbackInFling && mState != RecyclerView.SCROLL_STATE_IDLE) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "在快速滑动时忽略选择更改回调 ");
                        }
                        mCallbackOnIdle = true;
                        return;
                    }
                    if (mOnItemSelectedListener != null) {
                        mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
                    }
                }
            }
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "onScrolled: dx:" + dx + ",dy:" + dy);
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            mState = newState;
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "onScrollStateChanged: " + newState);
            }
            if (mState == RecyclerView.SCROLL_STATE_IDLE) {
                View snap = mSnapHelper.findSnapView(recyclerView.getLayoutManager());
                if (snap != null) {
                    int selectedPosition = recyclerView.getLayoutManager().getPosition(snap);
                    if (selectedPosition != mCurSelectedPosition) {
                        if (mCurSelectedView != null) {
                            mCurSelectedView.setSelected(false);
                        }
                        mCurSelectedView = snap;
                        mCurSelectedView.setSelected(true);
                        mCurSelectedPosition = selectedPosition;
                        if (mOnItemSelectedListener != null) {
                            mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
                        }
                    } else if (!mCallbackInFling && mOnItemSelectedListener != null && mCallbackOnIdle) {
                        mCallbackOnIdle = false;
                        mOnItemSelectedListener.onItemSelected(recyclerView, snap, mCurSelectedPosition);
                    }
                } else {
                    Log.e(TAG, "onScrollStateChanged: snap null");
                }
            }
        }
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        GallerySmoothScroller linearSmoothScroller = new GallerySmoothScroller(recyclerView.getContext());
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    @Override
    public void scrollToPosition(int position) {
        computeScrollVectorForPosition(position);
    }

    /**
     * 实现支持 {@link GalleryLayoutManager#smoothScrollToPosition(RecyclerView, RecyclerView.State, int)}
     */
    private class GallerySmoothScroller extends LinearSmoothScroller {

        public GallerySmoothScroller(Context context) {
            super(context);
        }

        /**
         * 计算使给定视图位于RecycleView中心所需的水平滚动量
         *
         * @param view 我们希望使其位于RecycleView中心的视图
         * @return 使视图位于RecycleView中心所需的水平滚动量
         */
        public int calculateDxToMakeCentral(View view) {
            final RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0;
            }
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            final int left = layoutManager.getDecoratedLeft(view) - params.leftMargin;
            final int right = layoutManager.getDecoratedRight(view) + params.rightMargin;
            final int start = layoutManager.getPaddingLeft();
            final int end = layoutManager.getWidth() - layoutManager.getPaddingRight();
            final int childCenter = left + (int) ((right - left) / 2.0f);
            final int containerCenter = (int) ((end - start) / 2.f);
            return containerCenter - childCenter;
        }

        /**
         * 计算使给定视图位于RecycleView中心所需的垂直滚动量
         *
         * @param view 我们希望使其位于RecycleView中心的视图
         * @return 使视图位于RecycleView中心所需的垂直滚动量
         */
        public int calculateDyToMakeCentral(View view) {
            final RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if (layoutManager == null || !layoutManager.canScrollVertically()) {
                return 0;
            }
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            final int top = layoutManager.getDecoratedTop(view) - params.topMargin;
            final int bottom = layoutManager.getDecoratedBottom(view) + params.bottomMargin;
            final int start = layoutManager.getPaddingTop();
            final int end = layoutManager.getHeight() - layoutManager.getPaddingBottom();
            final int childCenter = top + (int) ((bottom - top) / 2.0f);
            final int containerCenter = (int) ((end - start) / 2.f);
            return containerCenter - childCenter;
        }

        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
            final int dx = calculateDxToMakeCentral(targetView);
            final int dy = calculateDyToMakeCentral(targetView);
            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
            final int time = calculateTimeForDeceleration(distance);
            if (time > 0) {
                action.update(-dx, -dy, time, mDecelerateInterpolator);
            }
        }
    }
}