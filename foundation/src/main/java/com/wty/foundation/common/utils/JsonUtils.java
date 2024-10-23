package com.wty.foundation.common.utils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Json 工具类
 */
public class JsonUtils {
    private static final String TAG = "JsonUtils";
    private static final Gson gson = new Gson();

    private JsonUtils() {}

    /**
     * 将JSON字符串转换为指定类型的实体对象
     *
     * @param json JSON字符串
     * @param clazz 目标类的Class对象
     * @param <T> 实体类型
     * @return 转换后的实体对象，如果转换失败返回null
     */
    @Nullable
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 将JSON字符串转换为指定类型的实体对象
     *
     * @param json JSON字符串
     * @param type 目标类型的TypeToken
     * @param <T> 实体类型
     * @return 转换后的实体对象，如果转换失败返回null
     */
    @Nullable
    public static <T> T fromJson(String json, TypeToken<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, type.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 将JsonElement转换为指定类型的实体对象
     *
     * @param element JsonElement对象
     * @param clazz 目标类的Class对象
     * @param <T> 实体类型
     * @return 转换后的实体对象，如果转换失败返回null
     */
    @Nullable
    public static <T> T fromJson(JsonElement element, Class<T> clazz) {
        if (element == null) {
            return null;
        }
        try {
            return gson.fromJson(element, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 将JsonElement转换为指定类型的实体对象
     *
     * @param element JsonElement对象
     * @param type 目标类型的TypeToken
     * @param <T> 实体类型
     * @return 转换后的实体对象，如果转换失败返回null
     */
    @Nullable
    public static <T> T fromJson(JsonElement element, TypeToken<T> type) {
        if (element == null) {
            return null;
        }
        try {
            return gson.fromJson(element, type.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 将实体对象转换为JSON字符串
     *
     * @param obj 实体对象
     * @param <T> 实体类型
     * @return JSON字符串，如果对象为null返回null
     */
    public static <T> String toJson(T obj) {
        return obj == null ? null : gson.toJson(obj);
    }

    /**
     * 将JSON字符串转换为List<T>
     *
     * @param json JSON字符串
     * @param <T> 集合数据类型
     * @return List<T>，如果转换失败返回空列表
     */
    @NonNull
    public static <T> List<T> fromJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<T>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为List<T>，其中T是具体的类型
     *
     * @param json JSON字符串
     * @param type 具体类型的TypeToken
     * @param <T> 列表元素的类型
     * @return List<T>，如果转换失败返回空列表
     */
    @NonNull
    public static <T> List<T> fromJsonList(String json, TypeToken<List<T>> type) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return gson.fromJson(json, type.getType());
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为Map<String, T>
     *
     * @param json JSON字符串
     * @param <T> 映射值的数据类型
     * @return Map<String, T>，如果转换失败返回空映射
     */
    @NonNull
    public static <T> Map<String, T> fromJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Type mapType = new TypeToken<Map<String, T>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyMap();
        }
    }

    /**
     * 将JSON字符串转换为HashMap<String, T>
     *
     * @param json JSON字符串
     * @param <T> 映射值的数据类型
     * @return HashMap<String, T>，如果转换失败返回空映射
     */
    @NonNull
    public static <T> HashMap<String, T> fromJsonToHashMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            Type mapType = new TypeToken<HashMap<String, T>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return new HashMap<>();
        }
    }

    /**
     * 将JSON字符串转换为LinkedHashMap<String, T>
     *
     * @param json JSON字符串
     * @param <T> 映射值的数据类型
     * @return LinkedHashMap<String, T>，如果转换失败返回空映射
     */
    @NonNull
    public static <T> LinkedHashMap<String, T> fromJsonToLinkedHashMap(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            Type mapType = new TypeToken<LinkedHashMap<String, T>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将JSON字符串转换为TreeMap<String, T>
     *
     * @param json JSON字符串
     * @param <T> 映射值的数据类型
     * @return TreeMap<String, T>，如果转换失败返回空映射
     */
    @NonNull
    public static <T> TreeMap<String, T> fromJsonToTreeMap(String json) {
        if (json == null || json.isEmpty()) {
            return new TreeMap<>();
        }
        try {
            Type mapType = new TypeToken<TreeMap<String, T>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return new TreeMap<>();
        }
    }

    /**
     * 将JSON字符串转换为ConcurrentHashMap<String, T>
     *
     * @param json JSON字符串
     * @param <T> 映射值的数据类型
     * @return ConcurrentHashMap<String, T>，如果转换失败返回空映射
     */
    @NonNull
    public static <T> ConcurrentHashMap<String, T> fromJsonToConcurrentHashMap(String json) {
        if (json == null || json.isEmpty()) {
            return new ConcurrentHashMap<>();
        }
        try {
            Type mapType = new TypeToken<ConcurrentHashMap<String, T>>() {}.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * 将JSON字符串转换为List<HashMap<String, Object>>
     *
     * @param json JSON字符串
     * @return List<HashMap<String, Object>>，如果转换失败返回空列表
     */
    @NonNull
    public static List<HashMap<String, Object>> fromJsonToListOfHashMaps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<HashMap<String, Object>>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为List<LinkedTreeMap<String, Object>>
     *
     * @param json JSON字符串
     * @return List<LinkedTreeMap<String, Object>>，如果转换失败返回空列表
     */
    @NonNull
    public static List<LinkedTreeMap<String, Object>> fromJsonToListOfLinkedTreeMaps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<LinkedTreeMap<String, Object>>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为List<LinkedHashMap<String, Object>>
     *
     * @param json JSON字符串
     * @return List<LinkedHashMap<String, Object>>，如果转换失败返回空列表
     */
    @NonNull
    public static List<LinkedHashMap<String, Object>> fromJsonToListOfLinkedHashMaps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<LinkedHashMap<String, Object>>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为List<TreeMap<String, Object>>
     *
     * @param json JSON字符串
     * @return List<TreeMap<String, Object>>，如果转换失败返回空列表
     */
    @NonNull
    public static List<TreeMap<String, Object>> fromJsonToListOfTreeMaps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<TreeMap<String, Object>>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为List<ConcurrentHashMap<String, Object>>
     *
     * @param json JSON字符串
     * @return List<ConcurrentHashMap<String, Object>>，如果转换失败返回空列表
     */
    @NonNull
    public static List<ConcurrentHashMap<String, Object>> fromJsonToListOfConcurrentHashMaps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<ConcurrentHashMap<String, Object>>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Collections.emptyList();
        }
    }

    /**
     * 将JSON字符串转换为JsonArray
     *
     * @param json JSON字符串
     * @return JsonArray，如果转换失败返回null
     */
    @Nullable
    public static JsonArray fromJsonToJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, JsonArray.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * 将JSON字符串转换为JsonObject
     *
     * @param json JSON字符串
     * @return JsonObject，如果转换失败返回null
     */
    @Nullable
    public static JsonObject fromJsonToJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, JsonObject.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}
