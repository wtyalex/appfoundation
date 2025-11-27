package com.wty.foundation.core.fragment;

import androidx.fragment.app.Fragment;

/**
 * @author wutianyu
 * @createTime 2023/2/15 16:57
 * @describe 可见性感知Fragment基类，支持监听Fragment显示/隐藏状态变化
 */
public class VisibilityFragment extends Fragment implements IFragmentVisibility {
    // 如果Fragment对用户可见，则为True
    private boolean mIsFragmentVisible = false;

    // 如果Fragment第一次对用户可见，则为True
    private boolean mIsFragmentVisibleFirst = true;

    public VisibilityFragment() {}

    public VisibilityFragment(int contentLayoutId) {
        super(contentLayoutId);
    }

    @Override
    public void onResume() {
        super.onResume();

        determineFragmentVisible();
    }

    @Override
    public void onPause() {
        super.onPause();

        determineFragmentInvisible();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            determineFragmentInvisible();
        } else {
            determineFragmentVisible();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            determineFragmentVisible();
        } else {
            determineFragmentInvisible();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mIsFragmentVisible = false;
        mIsFragmentVisibleFirst = true;
    }

    public boolean isVisibleToUser() {
        return mIsFragmentVisible;
    }

    private void determineFragmentVisible() {
        Fragment parent = getParentFragment();
        if (parent != null && parent instanceof VisibilityFragment) {
            if (!((VisibilityFragment) parent).isVisibleToUser()) {
                // 父Fragment不可见，子Fragment必须不可见
                return;
            }
        }

        if (isResumed() && !isHidden() && getUserVisibleHint() && !mIsFragmentVisible) {
            mIsFragmentVisible = true;
            onVisible();
            if (mIsFragmentVisibleFirst) {
                mIsFragmentVisibleFirst = false;
                onVisibleFirst();
            } else {
                onVisibleExceptFirst();
            }
            determineChildFragmentVisible();
        }
    }

    private void determineFragmentInvisible() {
        if (mIsFragmentVisible) {
            mIsFragmentVisible = false;
            onInvisible();
            determineChildFragmentInvisible();
        }
    }

    private void determineChildFragmentVisible() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof VisibilityFragment) {
                ((VisibilityFragment) fragment).determineChildFragmentVisible();
            }
        }

    }

    private void determineChildFragmentInvisible() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof VisibilityFragment) {
                ((VisibilityFragment) fragment).determineChildFragmentInvisible();
            }
        }
    }
}
