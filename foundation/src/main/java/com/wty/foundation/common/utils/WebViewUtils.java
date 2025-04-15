package com.wty.foundation.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 用于管理WebView的初始化、生命周期、JavaScript交互等。
 * 支持自定义WebViewClient、WebChromeClient、文件下载、Cookie管理等功能。
 */
public class WebViewUtils {
    private static final String TAG = "WebViewUtils";
    private WebView mWebView;
    private Context mContext;
    private WebViewClient mCustomWebViewClient;
    private WebChromeClient mCustomWebChromeClient;
    private OnPageLoadListener mPageLoadListener;
    private OnDownloadListener mDownloadListener;
    private OnSslErrorListener mSslErrorListener;

    /**
     * 构造函数，初始化WebView
     *
     * @param context 上下文对象
     */
    public WebViewUtils(@NonNull Context context) {
        this.mContext = context;
        initWebView();
    }

    /**
     * 初始化WebView及其相关设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        mWebView = new WebView(mContext);

        // WebView基础设置
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 启用JavaScript
        webSettings.setDomStorageEnabled(true); // 启用DOM存储
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // 设置缓存模式
        webSettings.setAllowFileAccess(false); // 禁止文件访问
        webSettings.setAllowContentAccess(false); // 禁止内容访问
        webSettings.setDatabaseEnabled(true); // 启用数据库
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕大小
        webSettings.setUseWideViewPort(true); // 使用宽视口
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);//禁止JavaScript自动打开新窗口（默认行为）

        // 兼容性设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // 允许混合内容
        }

        // 优先使用自定义 WebViewClient
        if (mCustomWebViewClient != null) {
            mWebView.setWebViewClient(mCustomWebViewClient);
        } else {
            mWebView.setWebViewClient(new InternalWebViewClient());
        }

        // 优先使用自定义 WebChromeClient
        if (mCustomWebChromeClient != null) {
            mWebView.setWebChromeClient(mCustomWebChromeClient);
        } else {
            mWebView.setWebChromeClient(new InternalWebChromeClient());
        }

        // 设置文件下载监听
        mWebView.setDownloadListener(new InternalDownloadListener());
    }

    /**
     * 获取WebView实例
     *
     * @return WebView实例，可能为null
     */
    @Nullable
    public WebView getWebView() {
        return mWebView;
    }

    /**
     * 加载指定URL
     *
     * @param url 要加载的URL
     */
    public void loadUrl(@NonNull String url) {
        if (mWebView != null) {
            mWebView.loadUrl(url);
        }
    }

    /**
     * 加载HTML字符串
     *
     * @param data     HTML数据
     * @param mimeType 数据的MIME类型
     * @param encoding 数据的编码格式
     */
    public void loadData(@NonNull String data, @Nullable String mimeType, @Nullable String encoding) {
        if (mWebView != null) {
            mWebView.loadData(data, mimeType, encoding);
        }
    }

    /**
     * 加载带有Base URL的HTML字符串
     *
     * @param baseUrl    基础URL
     * @param data       HTML数据
     * @param mimeType   数据的MIME类型
     * @param encoding   数据的编码格式
     * @param historyUrl 历史URL
     */
    public void loadDataWithBaseURL(@Nullable String baseUrl, @NonNull String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
        }
    }

    /**
     * 设置自定义WebViewClient
     *
     * @param webViewClient 自定义的WebViewClient
     */
    public void setCustomWebViewClient(@Nullable WebViewClient webViewClient) {
        this.mCustomWebViewClient = webViewClient;
        if (mWebView != null) {
            mWebView.setWebViewClient(webViewClient != null ? webViewClient : new InternalWebViewClient());
        }
    }

    /**
     * 设置自定义WebChromeClient
     *
     * @param webChromeClient 自定义的WebChromeClient
     */
    public void setCustomWebChromeClient(@Nullable WebChromeClient webChromeClient) {
        this.mCustomWebChromeClient = webChromeClient;
        if (mWebView != null) {
            mWebView.setWebChromeClient(webChromeClient != null ? webChromeClient : new InternalWebChromeClient());
        }
    }

    /**
     * 设置页面加载监听器
     *
     * @param listener 页面加载监听器
     */
    public void setOnPageLoadListener(@Nullable OnPageLoadListener listener) {
        this.mPageLoadListener = listener;
    }

    /**
     * 设置文件下载监听器
     *
     * @param listener 文件下载监听器
     */
    public void setOnDownloadListener(@Nullable OnDownloadListener listener) {
        this.mDownloadListener = listener;
    }

    /**
     * 设置SSL错误监听器
     *
     * @param listener SSL错误监听器
     *                 注意：使用此接口可能会引入安全风险，请谨慎使用。
     */
    public void setOnSslErrorListener(OnSslErrorListener listener) {
        this.mSslErrorListener = listener;
    }

    /**
     * 同步Cookie到WebView
     *
     * @param url    要同步Cookie的URL
     * @param cookie 要同步的Cookie字符串
     */
    public void syncCookie(@NonNull String url, @NonNull String cookie) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(url, cookie);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0及以上版本，使用flush()方法
            cookieManager.flush();
        } else {
            // Android 5.0以下版本，使用CookieSyncManager
            CookieSyncManager.createInstance(mContext);
            CookieSyncManager.getInstance().sync();
            Log.w(TAG, "在Android 5.0以下版本，同步Cookie后建议手动调用 WebView.loadUrl 来触发Cookie的同步。");
        }
    }

    /**
     * 清除Cookie
     */
    public void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
    }

    /**
     * 释放WebView资源
     */
    public void destroy() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.setWebViewClient(null);
            mWebView.setWebChromeClient(null);
            mWebView.destroy();
            mWebView = null;
            // 清理Cookie
            clearCookies();
        }
    }

    /**
     * 处理Activity或Fragment的onResume生命周期
     */
    public void onResume() {
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    /**
     * 处理Activity或Fragment的onPause生命周期
     */
    public void onPause() {
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    /**
     * 添加JavaScript接口
     *
     * @param object 要暴露给JavaScript的对象
     * @param name   接口名称
     */
    @SuppressLint("JavascriptInterface")
    public void addJavascriptInterface(@NonNull Object object, @NonNull String name) {
        if (mWebView != null) {
            mWebView.addJavascriptInterface(object, name);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        if (mWebView != null) {
            mWebView.clearCache(true);
        }
    }

    /**
     * 清除历史记录
     */
    public void clearHistory() {
        if (mWebView != null) {
            mWebView.clearHistory();
        }
    }

    /**
     * 执行JavaScript代码
     *
     * @param script 要执行的JavaScript代码
     *               注意：该方法仅支持API 19及以上版本，低版本无法处理返回值。
     */
    public void evaluateJavascript(@NonNull String script) {
        if (mWebView != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWebView.evaluateJavascript(script, null);
                } else {
                    Log.w(TAG, "evaluateJavascript方法仅支持API 19及以上版本，当前版本无法处理返回值，将直接加载JavaScript代码。");
                    mWebView.loadUrl("javascript:" + script);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error evaluating JavaScript: " + e.getMessage());
            }
        }
    }

    /**
     * 设置是否启用缩放功能
     *
     * @param enabled 是否启用
     */
    public void setZoomEnabled(boolean enabled) {
        if (mWebView != null) {
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setBuiltInZoomControls(enabled);
            webSettings.setDisplayZoomControls(enabled);
        }
    }

    /**
     * 设置WebView是否支持多窗口
     *
     * @param support 是否支持
     */
    public void setSupportMultipleWindows(boolean support) {
        if (mWebView != null) {
            mWebView.getSettings().setSupportMultipleWindows(support);
        }
    }

    /**
     * 设置WebView是否支持手势缩放
     *
     * @param support 是否支持
     */
    public void setSupportZoom(boolean support) {
        if (mWebView != null) {
            mWebView.getSettings().setSupportZoom(support);
        }
    }

    /**
     * 设置WebView是否允许访问文件系统
     *
     * @param allow 是否允许
     */
    public void setAllowFileAccess(boolean allow) {
        if (mWebView != null) {
            mWebView.getSettings().setAllowFileAccess(allow);
        }
    }

    /**
     * 设置WebView是否允许访问内容
     *
     * @param allow 是否允许
     */
    public void setAllowContentAccess(boolean allow) {
        if (mWebView != null) {
            mWebView.getSettings().setAllowContentAccess(allow);
        }
    }

    /**
     * 设置WebView是否启用地理定位
     *
     * @param enabled 是否启用
     */
    public void setGeolocationEnabled(boolean enabled) {
        if (mWebView != null) {
            mWebView.getSettings().setGeolocationEnabled(enabled);
        }
    }

    /**
     * 设置WebView是否启用JavaScript弹窗
     *
     * @param enabled 是否启用
     */
    public void setJavaScriptCanOpenWindowsAutomatically(boolean enabled) {
        if (mWebView != null) {
            mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(enabled);
        }
    }

    /**
     * 设置WebView是否启用自动加载图片
     *
     * @param enabled 是否启用
     */
    public void setLoadsImagesAutomatically(boolean enabled) {
        if (mWebView != null) {
            mWebView.getSettings().setLoadsImagesAutomatically(enabled);
        }
    }

    /**
     * 设置WebView是否启用本地存储
     *
     * @param enabled 是否启用
     */
    public void setDomStorageEnabled(boolean enabled) {
        if (mWebView != null) {
            mWebView.getSettings().setDomStorageEnabled(enabled);
        }
    }

    /**
     * 设置WebView是否启用数据库存储
     *
     * @param enabled 是否启用
     */
    public void setDatabaseEnabled(boolean enabled) {
        if (mWebView != null) {
            mWebView.getSettings().setDatabaseEnabled(enabled);
        }
    }

    /**
     * 设置WebView的缓存模式
     *
     * @param mode 缓存模式
     */
    public void setCacheMode(int mode) {
        if (mWebView != null) {
            mWebView.getSettings().setCacheMode(mode);
        }
    }

    /**
     * 设置WebView是否启用硬件加速
     *
     * @param enabled 是否启用
     */
    public void setHardwareAccelerationEnabled(boolean enabled) {
        if (mWebView != null) {
            if (enabled) {
                mWebView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
            } else {
                mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    // 内部WebViewClient，处理默认逻辑
    private class InternalWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (mPageLoadListener != null) {
                mPageLoadListener.onPageStarted(url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mPageLoadListener != null) {
                mPageLoadListener.onPageFinished(url);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (mPageLoadListener != null && request.isForMainFrame()) {
                String errorDescription = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? error.getDescription().toString() : "Unknown error";
                mPageLoadListener.onPageError(errorDescription);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (mSslErrorListener != null && mSslErrorListener.onSslError(error)) {
                handler.proceed();
            } else {
                // 默认拒绝处理SSL错误
                handler.cancel();
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return super.shouldInterceptRequest(view, request);
            } else {
                return super.shouldInterceptRequest(view, request.getUrl().toString());
            }
        }
    }

    // 内部WebChromeClient，处理默认逻辑
    private class InternalWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (mPageLoadListener != null) {
                mPageLoadListener.onProgressChanged(newProgress);
            }
        }
    }

    // 内部DownloadListener，处理文件下载
    private class InternalDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength);
            }
        }
    }

    /**
     * 页面加载监听器接口
     */
    public interface OnPageLoadListener {
        /**
         * 页面开始加载回调
         *
         * @param url 开始加载的页面URL
         */
        void onPageStarted(String url);

        /**
         * 页面加载完成回调
         *
         * @param url 加载完成的页面URL
         */
        void onPageFinished(String url);

        /**
         * 页面加载出错回调
         *
         * @param errorDescription 错误描述信息
         */
        void onPageError(String errorDescription);

        /**
         * 页面加载进度变化回调
         *
         * @param progress 加载进度
         */
        void onProgressChanged(int progress);
    }

    /**
     * 文件下载监听器接口
     */
    public interface OnDownloadListener {
        /**
         * 文件下载开始回调
         *
         * @param url                下载文件的URL
         * @param userAgent          用户代理
         * @param contentDisposition 内容配置
         * @param mimetype           文件的MIME类型
         * @param contentLength      文件长度
         */
        void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength);
    }

    /**
     * SSL错误监听器接口
     */
    public interface OnSslErrorListener {
        /**
         * 处理SSL错误
         *
         * @param error SSL错误对象
         * @return 是否继续加载页面
         */
        boolean onSslError(SslError error);
    }
}