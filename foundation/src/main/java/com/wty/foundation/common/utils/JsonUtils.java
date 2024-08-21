package com.wty.foundation.common.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Json 工具类
 */
public class JsonUtils {
    private static final String TAG = "JsonUtils";
    private static Gson gson = new Gson();

    private JsonUtils() {}

    /**
     * Json转实体类
     *
     * @param json String
     * @param clazz Class
     * @param <T> 实体类型
     * @return 实体数据
     */
    @Nullable
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Json转实体类
     *
     * @param element String
     * @param clazz Class
     * @param <T> 实体类型
     * @return 实体数据
     */
    @Nullable
    public static <T> T fromJson(JsonElement element, Class<T> clazz) {
        try {
            return gson.fromJson(element, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Json转实体类
     *
     * @param element String
     * @param clazz Class
     * @param <T> 实体类型
     * @return 实体数据
     */
    @Nullable
    public static <T> T fromJson(JsonElement element, TypeToken<T> clazz) {
        try {
            return gson.fromJson(element, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 实体转Json字符串
     *
     * @param obj 实体
     * @param <T> 实体类型
     * @return Json字符串
     */
    public static <T> String toJson(T obj) {
        return obj == null ? null : gson.toJson(obj);
    }

    /**
     * Json字符串转实体List
     *
     * @param json String
     * @param <T> 集合数据类型
     * @return List<T>
     */
    @NonNull
    public static <T> List<T> fromJsonList(String json) {
        try {
            TypeToken<List<T>> typeToken = new TypeToken<List<T>>() {};
            return gson.fromJson(json, typeToken);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Json字符串转实体List
     *
     * @param json String
     * @param <T> 集合数据类型
     * @return List<T>
     */
    @NonNull
    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        try {
            return gson.fromJson(json, typeToken);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Json字符串转实体Map
     *
     * @param json String
     * @param <T> 集合数据类型
     * @return Map<String, T>
     */
    @NonNull
    public static <T> Map<String, T> fromJsonMap(String json) {
        try {
            TypeToken<Map<String, T>> typeToken = new TypeToken<Map<String, T>>() {};
            return gson.fromJson(json, typeToken);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.EMPTY_MAP;
        }
    }
}
