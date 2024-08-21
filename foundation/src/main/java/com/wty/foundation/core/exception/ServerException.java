package com.wty.foundation.core.exception;

/**
 * @author wutianyu
 * @sinc 2023/1/18 9:38
 */
public class ServerException extends RuntimeException {
    private final int mCode;

    public ServerException(int code, String message) {
        super(message);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }
}
