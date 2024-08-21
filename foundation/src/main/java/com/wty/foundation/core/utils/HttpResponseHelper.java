package com.wty.foundation.core.utils;

import com.wty.foundation.core.exception.ServerException;
import com.wty.foundation.core.http.Response;

import android.util.Log;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author wutianyu
 * @createTime 2023/2/3 11:43
 * @describe 帮助处理网络请求后的模板数据
 */
public class HttpResponseHelper {
    /**
     * 处理网络请求结果
     *
     * @param observable 网络请求Observable<Response<T>>
     * @param taskResult 结果回调
     * @param <T> 实体类
     */
    public static <T> void doResult(Observable<Response<T>> observable, TaskResult<Response<T>> taskResult) {
        doResult(observable, true, taskResult);
    }

    /**
     * 处理网络请求结果
     *
     * @param observable 网络请求Observable<Response<T>>
     * @param isMain true回调在主线程，false在IO线程
     * @param taskResult 结果回调
     * @param <T> 实体类
     */
    public static <T> void doResult(Observable<Response<T>> observable, boolean isMain,
        TaskResult<Response<T>> taskResult) {
        observable = observable.subscribeOn(Schedulers.io());
        if (isMain) {
            observable = observable.observeOn(AndroidSchedulers.mainThread());
        }
        observable.subscribe(tResponse -> {
            if (!tResponse.isSuccess()) {
                tResponse.setError(new ServerException(tResponse.getCode(), tResponse.getMsg()));
            }
            taskResult.onResult(tResponse);
        }, throwable -> {
            Response<T> response = new Response<>();
            response.setCode(-1);
            response.setError(throwable);
            taskResult.onResult(response);
            Log.e("Response", "error:", throwable);
        });
    }

}
