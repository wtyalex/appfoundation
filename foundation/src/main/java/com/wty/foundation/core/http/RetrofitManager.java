package com.wty.foundation.core.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author: wutianyu
 * @date: 2023/1/11 Retrofit工具类
 *        <p>
 *        1、{@link RetrofitManager}使用静态内部类的单例模式 2、使用 {@link #getInstance()}方法获取单例 3、使用单例的{@link #create(Class)}方法获取API对象
 */
public class RetrofitManager {
    private Retrofit mRetrofit;

    private static final class SingleHolder {
        private static final RetrofitManager _INSTANCE = new RetrofitManager();
    }

    public static RetrofitManager getInstance() {
        return SingleHolder._INSTANCE;
    }

    public void init(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config is null");
        }
        mRetrofit = new Retrofit.Builder()
            // 添加自动gson解析
            .addConverterFactory(GsonConverterFactory.create())
            // 让Retrofit支持RxJava2
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            // 设置公共的url
            .baseUrl(config.getBaseUrl())
            // 配置自己的okhttpClinet
            .client(buildOkhttpClient(config)).build();
    }

    private RetrofitManager() {

    }

    private OkHttpClient buildOkhttpClient(Config config) {

        // 构造一个OkHttpClient对应
        OkHttpClient.Builder builder =
            new OkHttpClient.Builder().connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeoutTimeoutMs(), TimeUnit.MILLISECONDS);
        if (config.getBaseUrl().startsWith("https://")) {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
            };
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, trustManager);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        for (Interceptor interceptor : config.getInterceptor()) {
            builder.addInterceptor(interceptor);
        }
        for (Interceptor interceptor : config.getNetworkInterceptor()) {
            builder.addNetworkInterceptor(interceptor);
        }
        return builder.build();
    }

    /**
     * 如果有其他的IApi，通过这个有参的方法创建
     */
    public <T> T create(final Class<T> service) {
        return mRetrofit.create(service);
    }
}
