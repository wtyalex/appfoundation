package com.wty.foundation.core.exception;

/**
 * @author wutianyu
 * @createTime 2023/1/30 9:27
 * @describe App自身业务异常
 */
public class AppException extends RuntimeException {
    private final int mCode;

    public AppException(String message) {
        this(-1, message);
    }

    public AppException(String message, Throwable throwable) {
        this(-2, message, throwable);
    }

    public AppException(int code, String message) {
        super(message);
        mCode = code;
    }

    public AppException(int code, String message, Throwable throwable) {
        super(message, throwable);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }
}
