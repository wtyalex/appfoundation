package com.wty.foundation.common.eventbus;

import java.util.HashSet;
import java.util.Set;

import org.greenrobot.eventbus.ThreadMode;

abstract class BaseSubscriber implements ISubscriber, Comparable<BaseSubscriber> {
    protected final Set<String> mActions = new HashSet<>();
    private MessageCallback mMessageCallback;
    private boolean isSticky;
    private int mPriority = 0;
    private volatile boolean isRegistered;

    @Override
    public ISubscriber setMsgHandler(MessageCallback callback) {
        if (isRegistered) {
            return this;
        }
        mMessageCallback = callback;
        return this;
    }

    @Override
    public ISubscriber addAction(String action) {
        if (isRegistered) {
            return this;
        }
        mActions.add(action);
        return this;
    }

    @Override
    public ISubscriber setSticky(boolean isSticky) {
        if (isRegistered) {
            return this;
        }
        this.isSticky = isSticky;
        return this;
    }

    @Override
    public ISubscriber setPriority(int priority) {
        if (isRegistered) {
            return this;
        }
        mPriority = priority;
        return this;
    }

    @Override
    public ISubscriber register() {
        if (!isRegistered) {
            isRegistered = true;
            SuperSubscriber.getInstance().register(this, mActions);
        }
        return this;
    }

    @Override
    public void unregister() {
        if (isRegistered) {
            isRegistered = false;
            SuperSubscriber.getInstance().unregister(this, mActions);
        }
    }

    abstract ThreadMode getThreadMode();

    boolean isSticky() {
        return isSticky;
    }

    void onMessage(MessageEvent event) {
        if (!isRegistered) {
            return;
        }
        if (mActions.contains(event.getAction()) && mMessageCallback != null) {
            mMessageCallback.onMsg(event);
        }
    }

    @Override
    public int compareTo(BaseSubscriber o) {
        return mPriority - o.mPriority;
    }
}
