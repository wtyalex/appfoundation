package com.wty.foundation.core.fragment;

import androidx.fragment.app.Fragment;

/**
 * @author wutianyu
 * @createTime 2023/2/15 16:57
 * @describe
 */
public class VisibilityFragment extends Fragment implements IFragmentVisibility {
    // True if the fragment is visible to the user.
    private boolean mIsFragmentVisible = false;

    // True if the fragment is visible to the user for the first time.
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
            if (!((VisibilityFragment)parent).isVisibleToUser()) {
                // Parent Fragment is invisible, child fragment must be invisible.
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
                ((VisibilityFragment)fragment).determineChildFragmentVisible();
            }
        }

    }

    private void determineChildFragmentInvisible() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof VisibilityFragment) {
                ((VisibilityFragment)fragment).determineChildFragmentInvisible();
            }
        }
    }
}
