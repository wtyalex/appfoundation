package com.wty.foundation.core.http;

import java.util.Collections;
import java.util.List;

import okhttp3.Interceptor;

/**
 * @author wutianyu
 * @createTime 2024/5/27 14:09
 * @describe
 */
public class Config {
    private String mBaseUrl;
    private long mConnectTimeoutMs;
    private long mReadTimeoutTimeoutMs;
    private long mWriteTimeoutTimeoutMs;
    private List<Interceptor> mNetworkInterceptor;
    private List<Interceptor> mInterceptor;

    private Config(Builder builder) {
        mBaseUrl = builder.mBaseUrl;
        mConnectTimeoutMs = builder.mConnectTimeoutMs;
        mReadTimeoutTimeoutMs = builder.mReadTimeoutTimeoutMs;
        mWriteTimeoutTimeoutMs = builder.mWriteTimeoutTimeoutMs;
        mNetworkInterceptor = builder.mNetworkInterceptor;
        mInterceptor = builder.mInterceptor;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public long getConnectTimeoutMs() {
        return mConnectTimeoutMs;
    }

    public long getReadTimeoutTimeoutMs() {
        return mReadTimeoutTimeoutMs;
    }

    public long getWriteTimeoutTimeoutMs() {
        return mWriteTimeoutTimeoutMs;
    }

    public List<Interceptor> getNetworkInterceptor() {
        return mNetworkInterceptor == null ? Collections.EMPTY_LIST : mNetworkInterceptor;
    }

    public List<Interceptor> getInterceptor() {
        return mInterceptor == null ? Collections.EMPTY_LIST : mInterceptor;
    }

    public static class Builder {
        private String mBaseUrl;
        private long mConnectTimeoutMs;
        private long mReadTimeoutTimeoutMs;
        private long mWriteTimeoutTimeoutMs;
        private List<Interceptor> mNetworkInterceptor;
        private List<Interceptor> mInterceptor;

        public Builder setBaseUrl(String mBaseUrl) {
            this.mBaseUrl = mBaseUrl;
            return this;
        }

        public Builder setConnectTimeoutMs(long mConnectTimeoutMs) {
            this.mConnectTimeoutMs = mConnectTimeoutMs;
            return this;
        }

        public Builder setReadTimeoutTimeoutMs(long mReadTimeoutTimeoutMs) {
            this.mReadTimeoutTimeoutMs = mReadTimeoutTimeoutMs;
            return this;
        }

        public Builder setWriteTimeoutTimeoutMs(long mWriteTimeoutTimeoutMs) {
            this.mWriteTimeoutTimeoutMs = mWriteTimeoutTimeoutMs;
            return this;
        }

        public Builder setNetworkInterceptor(List<Interceptor> mNetworkInterceptor) {
            this.mNetworkInterceptor = mNetworkInterceptor;
            return this;
        }

        public Builder setInterceptor(List<Interceptor> mInterceptor) {
            this.mInterceptor = mInterceptor;
            return this;
        }

        public Config builder() {
            return new Config(this);
        }

    }
}
