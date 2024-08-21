package com.wty.foundation.core.fragment;

interface IFragmentVisibility {

    /**
     * Called when the fragment is visible.
     */
    default void onVisible() {}

    /**
     * Called when the Fragment is not visible.
     */
    default void onInvisible() {}

    /**
     * Called when the fragment is visible for the first time.
     */
    default void onVisibleFirst() {}

    /**
     * Called when the fragment is visible except first time.
     */
    default void onVisibleExceptFirst() {}

    /**
     * Return true if the fragment is currently visible to the user.
     */
    boolean isVisibleToUser();
}