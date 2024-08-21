package com.wty.foundation.core.utils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import com.wty.foundation.core.exception.AppException;
import com.wty.foundation.core.exception.ServerException;

import android.util.Log;

import retrofit2.HttpException;

/**
 * @author wutianyu
 * @createTime 2023/1/18 9:32
 * @describe 根据异常类型转换成String的工具类
 */
public class ThrowableMsgUtils {
    private static final String TAG = "ThrowableMsgUtils";

    private ThrowableMsgUtils() {}

    /**
     * 把异常转成提示信息
     *
     * @param throwable Throwable
     * @return 提示信息
     */
    public static String getMsgByThrowable(Throwable throwable) {
        if (throwable instanceof HttpException) {
            int code = ((HttpException)throwable).code();
            if (code >= 500 && code < 600) {
                return "服务器异常：" + code + " " + throwable.getMessage();
            } else {
                return "网络异常：" + code;
            }
        } else if (throwable instanceof ConnectException) {
            return "网络连接异常";
        } else if (throwable instanceof SocketTimeoutException) {
            return "网络连接超时";
        } else if (throwable instanceof ServerException) {
            ServerException exception = (ServerException)throwable;
            if (exception.getCode() == -2) {
                return "";
            }
            return throwable.getMessage();
        } else if (throwable instanceof AppException) {
            return throwable.getMessage();
        } else {
            Log.e(TAG, Log.getStackTraceString(throwable));
            return "未知错误";
        }

    }
}
