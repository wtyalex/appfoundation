package com.wty.foundation.core.fragment;

/**
 * @author wutianyu
 * @createTime 2023/1/15
 * @describe Fragment可见性监听接口，提供Fragment显示/隐藏状态回调方法
 */
interface IFragmentVisibility {

    /**
     * 当Fragment可见时调用
     */
    default void onVisible() {}

    /**
     * 当Fragment不可见时调用
     */
    default void onInvisible() {}

    /**
     * 当Fragment第一次可见时调用
     */
    default void onVisibleFirst() {}

    /**
     * 当Fragment除了第一次之外可见时调用
     */
    default void onVisibleExceptFirst() {}

    /**
     * 返回Fragment当前是否对用户可见
     */
    boolean isVisibleToUser();
}