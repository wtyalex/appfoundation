package com.wty.foundation.core.exception;

/**
 * @author wutianyu
 * @createTime 2023/1/18
 * @describe 服务器异常类，用于封装服务器返回的错误码和错误信息
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
