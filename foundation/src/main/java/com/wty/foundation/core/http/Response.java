package com.wty.foundation.core.http;

/**
 * @author wutianyu
 * @createTime 2023/1/16
 * @describe 网络请求响应数据封装类，包含状态码、消息和返回数据
 */
public class Response<T> {
    /**
     * 返回码
     */
    private int rtnCode;
    /**
     * 信息描述
     */
    private String rtnMsg;
    /**
     * 返回数据
     */
    private T rtnData;

    private Throwable error;

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public int getCode() {
        return rtnCode;
    }

    public T getData() {
        return rtnData;
    }

    public void setCode(int rtnCode) {
        this.rtnCode = rtnCode;
    }

    public void setData(T rtnData) {
        this.rtnData = rtnData;
    }

    public String getMsg() {
        return rtnMsg;
    }

    public void setMsg(String msg) {
        this.rtnMsg = msg;
    }

    public boolean isSuccess() {
        return rtnCode == 0;
    }

    /**
     * 根据参数创建一个新的不包含rtnData值的Response对象
     * 
     * @param response Response
     * @param <T> rtnData的类型
     * @return Response
     */
    public static <T> Response<T> create(Response<?> response) {

        Response<T> result = new Response<>();
        if (response == null) {
            return result;
        }
        result.setCode(response.getCode());
        result.setMsg(response.getMsg());
        result.setError(response.getError());
        return result;
    }
}
