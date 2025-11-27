package com.wty.foundation.core.http;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author wutianyu
 * @createTime 2023/1/11
 * @describe Retrofit管理器，提供网络请求接口的创建和配置管理功能
 */
public class RetrofitManager {
    // 默认Retrofit实例
    private Retrofit mDefaultRetrofit;
    // 配置信息
    private Config mConfig;
    // 缓存不同BaseUrl对应的Retrofit实例
    private final Map<String, Retrofit> retrofitCache = new ConcurrentHashMap<>();

    /**
     * 单例持有类
     */
    private static final class SingleHolder {
        private static final RetrofitManager _INSTANCE = new RetrofitManager();
    }

    /**
     * 获取单例实例
     * @return RetrofitManager实例
     */
    public static RetrofitManager getInstance() {
        return SingleHolder._INSTANCE;
    }

    /**
     * 私有构造方法，防止外部实例化
     */
    private RetrofitManager() {
    }

    /**
     * 初始化方法，必须先调用
     * @param config 配置信息对象，不能为null
     * @throws IllegalArgumentException 当config为null时抛出
     */
    public void init(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config is null");
        }
        this.mConfig = config;
        this.mDefaultRetrofit = buildRetrofit(config.getBaseUrl());
    }

    /**
     * 构建Retrofit实例
     * @param baseUrl 基础URL，必须以http://或https://开头
     * @return 构建好的Retrofit实例
     * @throws IllegalArgumentException 当baseUrl格式不正确时抛出
     */
    private Retrofit buildRetrofit(String baseUrl) {
        if (baseUrl == null || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("BaseUrl must start with http:// or https:// : " + baseUrl);
        }

        OkHttpClient client = buildOkhttpClient(mConfig);
        return new Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()).build();
    }

    /**
     * 构建OkHttpClient实例
     * 配置超时时间、SSL证书信任和拦截器
     * @param config 配置信息
     * @return 构建好的OkHttpClient实例
     * @throws RuntimeException 当SSL配置失败时抛出
     */
    private OkHttpClient buildOkhttpClient(Config config) {
        try {
            // 创建信任所有证书的TrustManager
            final X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            };

            // 初始化SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());

            // 构建OkHttpClient
            OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS).readTimeout(config.getReadTimeoutTimeoutMs(), TimeUnit.MILLISECONDS).writeTimeout(config.getWriteTimeoutTimeoutMs(), TimeUnit.MILLISECONDS).sslSocketFactory(sslContext.getSocketFactory(), trustAllManager).hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // 信任所有主机名
                }
            });

            // 添加应用拦截器
            for (Interceptor interceptor : config.getInterceptor()) {
                builder.addInterceptor(interceptor);
            }
            // 添加网络拦截器
            for (Interceptor interceptor : config.getNetworkInterceptor()) {
                builder.addNetworkInterceptor(interceptor);
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建API服务接口实例（使用默认BaseUrl）
     * @param service 接口类
     * @param <T> 接口类型
     * @return 接口实例
     * @throws IllegalStateException 当未初始化时抛出
     */
    public <T> T create(final Class<T> service) {
        if (mDefaultRetrofit == null) {
            throw new IllegalStateException("RetrofitManager is not initialized, call init() first.");
        }
        return mDefaultRetrofit.create(service);
    }

    /**
     * 创建API服务接口实例（使用指定BaseUrl）
     * 会缓存相同BaseUrl的Retrofit实例
     * @param service 接口类
     * @param baseUrl 指定的基础URL
     * @param <T> 接口类型
     * @return 接口实例
     * @throws IllegalStateException 当未初始化时抛出
     */
    public <T> T create(final Class<T> service, String baseUrl) {
        if (mConfig == null) {
            throw new IllegalStateException("RetrofitManager is not initialized, call init() first.");
        }
        Retrofit retrofit = retrofitCache.get(baseUrl);
        if (retrofit == null) {
            synchronized (retrofitCache) {
                retrofit = retrofitCache.get(baseUrl);
                if (retrofit == null) {
                    retrofit = buildRetrofit(baseUrl);
                    retrofitCache.put(baseUrl, retrofit);
                }
            }
        }
        return retrofit.create(service);
    }
}
