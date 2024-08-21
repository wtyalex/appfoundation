package com.wty.foundation.account.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.wty.foundation.account.AccountAuthority;
import com.wty.foundation.common.init.ActivityLifecycleManager;
import com.wty.foundation.common.utils.MD5;
import com.wty.foundation.common.utils.StringUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

/**
 * @author wutianyu
 * @createTime 2023/11/2 16:44
 * @describe
 */
public class TokenInterceptor implements Interceptor {
    private static final String TAG = "TokenInterceptor";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient mClient;

    public TokenInterceptor() {
        mClient = new OkHttpClient.Builder().build();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (response.isSuccessful() && ActivityLifecycleManager.getInstance().isForeground()) {
            ResponseBody body = response.body();
            if (body != null) {
                BufferedSource source = body.source();
                source.request(Long.MAX_VALUE);
                Buffer buffer = source.getBuffer();
                if ("gzip".equalsIgnoreCase(response.header("Content-Encoding"))) {
                    GzipSource gzipSource = new GzipSource(buffer.clone());
                    buffer = new Buffer();
                    buffer.writeAll(gzipSource);
                }

                MediaType contentType = body.contentType();
                Charset charset;
                if (contentType == null) {
                    charset = Charset.forName("UTF-8");
                } else {
                    Charset temp = contentType.charset(Charset.forName("UTF-8"));
                    if (temp == null) {
                        charset = Charset.forName("UTF-8");
                    } else {
                        charset = temp;
                    }
                }
                String bodyStr = buffer.clone().readString(charset);
                if (bodyStr.contains("\"rtnCode\":-2,")) {
                    String id = String.valueOf(SystemClock.elapsedRealtime());
                    Log.i(TAG, "GetNewToken:" + id);
                    final ConcurrentHashMap<String, String> token = new ConcurrentHashMap<>();
                    token.put("token", chain.request().header("token"));
                    CountDownLatch latch = new CountDownLatch(1);
                    mHandler.post(() -> {
                        AccountAuthority.getInstance().startAccountActivity(result -> {
                            if (!StringUtils.isNullEmpty(result)) {
                                token.put("token", result);
                            }
                            Log.i(TAG, "GetNewToken result:" + id);
                            latch.countDown();
                        });
                    });
                    try {
                        latch.await(60, TimeUnit.SECONDS);
                        Log.i(TAG, "GetNewToken End:" + id);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "latch.await: " + id, e);
                    }
                    Request.Builder builder = chain.request().newBuilder();
                    String tokenStr = token.get("token");
                    String SecretKey = chain.request().header("SecretKey");
                    builder.removeHeader("token");
                    builder.removeHeader("dateTime");
                    builder.removeHeader("tokenEncrypt");
                    Calendar calendar = Calendar.getInstance();
                    String dateTime = String.valueOf(calendar.getTimeInMillis());
                    builder.addHeader("dateTime", dateTime);
                    builder.addHeader("tokenEncrypt", getTokenEncrypt(tokenStr, SecretKey, dateTime));
                    builder.addHeader("token", tokenStr);
                    Response temp = mClient.newCall(builder.build()).execute();

                    response =
                        response.newBuilder().body(temp.body()).message(temp.message()).code(temp.code()).build();
                    return response;
                }
            }
        }
        return response;
    }

    private static String getTokenEncrypt(String token, String key, String date) {
        String result = "";
        try {
            result = MD5.md5(token + key + date);
        } catch (Exception e) {

        }
        return result;
    }
}
