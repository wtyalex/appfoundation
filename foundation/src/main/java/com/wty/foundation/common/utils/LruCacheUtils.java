package com.wty.foundation.common.utils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.disklrucache.DiskLruCache;
import com.wty.foundation.common.init.AppContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 磁盘LRU缓存工具类
 * 支持基本数据类型、字符串、JSON对象/数组、Map、序列化对象及Parceler对象的缓存管理
 * 基于DiskLruCache实现，提供线程安全的缓存操作，采用单例模式和线程池管理IO任务
 */
public class LruCacheUtils {
    private static final String TAG = "LruCacheUtils";
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final long MAX_CACHE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int IO_BUFFER_SIZE = 8 * 1024; // 8KB
    private static final int TRANSACTION_TIMEOUT_SECONDS = 30; // 30秒超时

    private static volatile LruCacheUtils instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, Future<?>> activeTasks = new ConcurrentHashMap<>();
    private DiskLruCache diskCache;
    private final File cacheDir;
    private final Context context;
    private final Gson gson = new Gson();

    /**
     * 私有构造方法，初始化缓存目录和DiskLruCache
     */
    private LruCacheUtils(Context context) {
        this.context = context.getApplicationContext();
        this.cacheDir = getDiskCacheDir(this.context, "dc_cache");
        initDiskCache();
    }

    /**
     * 获取单例实例
     *
     * @return 单例对象
     */
    public static synchronized LruCacheUtils getInstance() {
        if (instance == null) {
            instance = new LruCacheUtils(AppContext.getInstance().getContext());
        }
        return instance;
    }

    /**
     * 初始化磁盘缓存
     */
    private void initDiskCache() {
        try {
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.w(TAG, "创建缓存目录失败: " + cacheDir.getAbsolutePath());
                return;
            }
            diskCache = DiskLruCache.open(cacheDir, APP_VERSION, VALUE_COUNT, MAX_CACHE_SIZE);
            Log.i(TAG, "磁盘缓存初始化成功: " + cacheDir.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "磁盘缓存初始化失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存目录（优先外部存储，其次内部存储）
     *
     * @param context    上下文
     * @param uniqueName 缓存目录唯一名称
     * @return 缓存目录文件
     */
    private File getDiskCacheDir(Context context, String uniqueName) {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null) {
            return new File(externalCacheDir, uniqueName);
        }
        return new File(context.getCacheDir(), uniqueName);
    }

    /**
     * 同步存入int类型缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putIntSync(String key, int value) {
        return putBytesSync(key, intToBytes(value));
    }

    /**
     * 同步获取int类型缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回0
     */
    public int getIntSync(String key) {
        byte[] data = getBytesSync(key);
        return data != null ? bytesToInt(data) : 0;
    }

    /**
     * 同步存入long类型缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putLongSync(String key, long value) {
        return putBytesSync(key, longToBytes(value));
    }

    /**
     * 同步获取long类型缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回0L
     */
    public long getLongSync(String key) {
        byte[] data = getBytesSync(key);
        return data != null ? bytesToLong(data) : 0L;
    }

    /**
     * 同步存入float类型缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putFloatSync(String key, float value) {
        return putBytesSync(key, floatToBytes(value));
    }

    /**
     * 同步获取float类型缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回0f
     */
    public float getFloatSync(String key) {
        byte[] data = getBytesSync(key);
        return data != null ? bytesToFloat(data) : 0f;
    }

    /**
     * 同步存入boolean类型缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putBooleanSync(String key, boolean value) {
        return putBytesSync(key, booleanToBytes(value));
    }

    /**
     * 同步获取boolean类型缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回false
     */
    public boolean getBooleanSync(String key) {
        byte[] data = getBytesSync(key);
        return data != null && bytesToBoolean(data);
    }

    /**
     * 同步存入字符串缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putStringSync(String key, String value) {
        if (value == null) return false;
        return putBytesSync(key, value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 同步获取字符串缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回null
     */
    public String getStringSync(String key) {
        byte[] data = getBytesSync(key);
        return data != null ? new String(data, StandardCharsets.UTF_8) : null;
    }

    /**
     * 同步存入JSONObject缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putJSONObjectSync(String key, JSONObject value) {
        if (value == null) return false;
        return putStringSync(key, value.toString());
    }

    /**
     * 同步获取JSONObject缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回null
     */
    public JSONObject getJSONObjectSync(String key) {
        String jsonStr = getStringSync(key);
        if (jsonStr == null) return null;
        try {
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            Log.e(TAG, "JSONObject解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入JSONArray缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    public boolean putJSONArraySync(String key, JSONArray value) {
        if (value == null) return false;
        return putStringSync(key, value.toString());
    }

    /**
     * 同步获取JSONArray缓存
     *
     * @param key 缓存键
     * @return 缓存值，失败返回null
     */
    public JSONArray getJSONArraySync(String key) {
        String jsonStr = getStringSync(key);
        if (jsonStr == null) return null;
        try {
            return new JSONArray(jsonStr);
        } catch (JSONException e) {
            Log.e(TAG, "JSONArray解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入Map缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <K>   Map的Key类型
     * @param <V>   Map的Value类型
     * @return 是否成功
     */
    public <K, V> boolean putMapSync(String key, Map<K, V> value) {
        if (value == null) return false;
        String json = gson.toJson(value);
        return putStringSync(key, json);
    }

    /**
     * 同步获取Map缓存
     *
     * @param key 缓存键
     * @param <K> Map的Key类型
     * @param <V> Map的Value类型
     * @return 缓存值，失败返回null
     */
    public <K, V> Map<K, V> getMapSync(String key) {
        String json = getStringSync(key);
        if (json == null) return null;
        try {
            TypeToken<Map<K, V>> typeToken = new TypeToken<Map<K, V>>() {
            };
            return gson.fromJson(json, typeToken.getType());
        } catch (Exception e) {
            Log.e(TAG, "Map解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入Serializable对象缓存
     *
     * @param key   缓存键
     * @param value 实现Serializable的对象
     * @return 是否成功
     */
    public boolean putSerializableSync(String key, Serializable value) {
        if (value == null) return false;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
            return putBytesSync(key, baos.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "对象序列化失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 同步获取Serializable对象缓存
     *
     * @param key   缓存键
     * @param clazz 对象类型
     * @param <T>   泛型类型
     * @return 缓存对象，失败返回null
     */
    public <T extends Serializable> T getSerializableSync(String key, Class<T> clazz) {
        byte[] data = getBytesSync(key);
        if (data == null) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object obj = ois.readObject();
            return clazz.cast(obj);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            Log.e(TAG, "对象反序列化失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入Gson序列化对象缓存
     *
     * @param key   缓存键
     * @param value 任意对象
     * @param <T>   泛型类型
     * @return 是否成功
     */
    public <T> boolean putObjectSync(String key, T value) {
        if (value == null) return false;
        String json = gson.toJson(value);
        return putStringSync(key, json);
    }

    /**
     * 同步获取Gson序列化对象缓存
     *
     * @param key   缓存键
     * @param clazz 对象类型
     * @param <T>   泛型类型
     * @return 缓存对象，失败返回null
     */
    public <T> T getObjectSync(String key, Class<T> clazz) {
        String json = getStringSync(key);
        if (json == null) return null;
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Log.e(TAG, "对象解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入Parceler对象缓存（需配合@Parcel注解）
     *
     * @param key   缓存键
     * @param value Parceler对象
     * @param <T>   泛型类型
     * @return 是否成功
     */
    public <T> boolean putParcelerSync(String key, T value) {
        if (value == null) return false;
        try {
            Parcelable parcelable = Parcels.wrap(value);
            return putParcelableSync(key, parcelable);
        } catch (Exception e) {
            Log.e(TAG, "Parceler序列化失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 同步获取Parceler对象缓存
     *
     * @param key 缓存键
     * @param <T> 泛型类型
     * @return 缓存对象，失败返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getParcelerSync(String key) {
        Parcelable parcelable = getParcelableSync(key);
        if (parcelable == null) return null;
        try {
            return (T) Parcels.unwrap(parcelable);
        } catch (Exception e) {
            Log.e(TAG, "Parceler反序列化失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 同步存入Parcelable对象缓存
     *
     * @param key   缓存键
     * @param value 实现Parcelable的对象
     * @return 是否成功
     */
    public boolean putParcelableSync(String key, Parcelable value) {
        if (value == null) return false;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Parcel parcel = Parcel.obtain();
            try {
                value.writeToParcel(parcel, 0);
                byte[] parcelData = parcel.marshall();
                baos.write(parcelData);
            } finally {
                parcel.recycle(); // 释放资源，防止内存泄漏
            }
            return putBytesSync(key, baos.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "Parcelable序列化失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 同步获取Parcelable对象缓存
     *
     * @param key 缓存键
     * @return 缓存对象，失败返回null
     */
    public Parcelable getParcelableSync(String key) {
        byte[] data = getBytesSync(key);
        if (data == null) return null;
        try {
            Parcel parcel = Parcel.obtain();
            try {
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0); // 重置读取位置
                return parcel.readParcelable(getClass().getClassLoader());
            } finally {
                parcel.recycle(); // 释放资源，防止内存泄漏
            }
        } catch (Exception e) {
            Log.e(TAG, "Parcelable反序列化失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 异步存入int缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param callback 结果回调
     */
    public void putInt(String key, int value, BasicCallback<Integer> callback) {
        submitTask(key, () -> {
            boolean success = putIntSync(key, value);
            notifyBasicCallback(callback, success, value);
        });
    }

    /**
     * 异步获取int缓存
     *
     * @param key      缓存键
     * @param callback 结果回调
     */
    public void getInt(String key, BasicCallback<Integer> callback) {
        submitTask(key, () -> {
            int value = getIntSync(key);
            notifyBasicCallback(callback, true, value);
        });
    }

    /**
     * 异步存入字符串缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param callback 结果回调
     */
    public void putString(String key, String value, BasicCallback<String> callback) {
        submitTask(key, () -> {
            boolean success = putStringSync(key, value);
            notifyBasicCallback(callback, success, value);
        });
    }

    /**
     * 异步获取字符串缓存
     *
     * @param key      缓存键
     * @param callback 结果回调
     */
    public void getString(String key, BasicCallback<String> callback) {
        submitTask(key, () -> {
            String value = getStringSync(key);
            notifyBasicCallback(callback, value != null, value);
        });
    }

    /**
     * 异步存入JSONObject缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param callback 结果回调
     */
    public void putJSONObject(String key, JSONObject value, BasicCallback<JSONObject> callback) {
        submitTask(key, () -> {
            boolean success = putJSONObjectSync(key, value);
            notifyBasicCallback(callback, success, value);
        });
    }

    /**
     * 异步获取JSONObject缓存
     *
     * @param key      缓存键
     * @param callback 结果回调
     */
    public void getJSONObject(String key, BasicCallback<JSONObject> callback) {
        submitTask(key, () -> {
            JSONObject value = getJSONObjectSync(key);
            notifyBasicCallback(callback, value != null, value);
        });
    }

    /**
     * 异步存入对象缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param callback 结果回调
     * @param <T>      泛型类型
     */
    public <T> void putObject(String key, T value, BasicCallback<T> callback) {
        submitTask(key, () -> {
            boolean success = putObjectSync(key, value);
            notifyBasicCallback(callback, success, value);
        });
    }

    /**
     * 异步获取对象缓存
     *
     * @param key      缓存键
     * @param clazz    对象类型
     * @param callback 结果回调
     * @param <T>      泛型类型
     */
    public <T> void getObject(String key, Class<T> clazz, BasicCallback<T> callback) {
        submitTask(key, () -> {
            T value = getObjectSync(key, clazz);
            notifyBasicCallback(callback, value != null, value);
        });
    }

    /**
     * 异步存入Parceler对象缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param callback 结果回调
     * @param <T>      泛型类型
     */
    public <T> void putParceler(String key, T value, BasicCallback<T> callback) {
        submitTask(key, () -> {
            boolean success = putParcelerSync(key, value);
            notifyBasicCallback(callback, success, value);
        });
    }

    /**
     * 异步获取Parceler对象缓存
     *
     * @param key      缓存键
     * @param callback 结果回调
     * @param <T>      泛型类型
     */
    public <T> void getParceler(String key, BasicCallback<T> callback) {
        submitTask(key, () -> {
            T value = getParcelerSync(key);
            notifyBasicCallback(callback, value != null, value);
        });
    }

    /**
     * 同步删除指定缓存
     *
     * @param key 缓存键
     * @return 是否成功
     */
    public boolean removeSync(String key) {
        if (key == null || diskCache == null) return false;
        try {
            String hashedKey = hashKeyForDisk(key);
            boolean success = diskCache.remove(hashedKey);
            diskCache.flush();
            Log.d(TAG, "缓存删除" + (success ? "成功" : "失败") + ": " + hashedKey);
            return success;
        } catch (IOException e) {
            Log.e(TAG, "删除缓存失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 异步删除指定缓存
     *
     * @param key      缓存键
     * @param callback 结果回调
     */
    public void remove(String key, EmptyCallback callback) {
        submitTask(key, () -> {
            boolean success = removeSync(key);
            notifyEmptyCallback(callback, success);
        });
    }

    /**
     * 同步清空所有缓存
     *
     * @return 是否成功
     */
    public boolean clearAllSync() {
        try {
            if (diskCache != null) {
                diskCache.delete();
                diskCache.close();
                diskCache = null;
            }
            initDiskCache();
            Log.i(TAG, "所有缓存已清空");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "清空缓存失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 异步清空所有缓存
     *
     * @param callback 结果回调
     */
    public void clearAll(EmptyCallback callback) {
        submitTask("clear_all", () -> {
            boolean success = clearAllSync();
            notifyEmptyCallback(callback, success);
        });
    }

    /**
     * 同步存入字节数组
     *
     * @param key  缓存键
     * @param data 字节数组
     * @return 是否成功
     */
    private boolean putBytesSync(String key, byte[] data) {
        if (key == null || data == null || diskCache == null) return false;

        String hashedKey = hashKeyForDisk(key);
        DiskLruCache.Editor editor = null;
        try {
            editor = diskCache.edit(hashedKey);
            if (editor == null) {
                Log.w(TAG, "获取编辑器失败: " + hashedKey);
                return false;
            }

            try (OutputStream os = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)) {
                os.write(data);
            }
            editor.commit();
            diskCache.flush();
            Log.d(TAG, "缓存存入成功: " + hashedKey);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "存入缓存失败: " + e.getMessage());
            if (editor != null) {
                try {
                    editor.abort();
                } catch (IOException ex) {
                    Log.e(TAG, "编辑器回滚失败: " + ex.getMessage());
                }
            }
            return false;
        }
    }

    /**
     * 同步获取字节数组
     *
     * @param key 缓存键
     * @return 字节数组，失败返回null
     */
    private byte[] getBytesSync(String key) {
        if (key == null || diskCache == null) return null;

        String hashedKey = hashKeyForDisk(key);
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskCache.get(hashedKey);
            if (snapshot == null) {
                Log.d(TAG, "缓存不存在: " + hashedKey);
                return null;
            }

            try (InputStream is = new BufferedInputStream(snapshot.getInputStream(0), IO_BUFFER_SIZE); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[IO_BUFFER_SIZE];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                Log.d(TAG, "缓存读取成功: " + hashedKey);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            Log.e(TAG, "读取缓存失败: " + e.getMessage());
            return null;
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
    }

    /**
     * 提交任务到线程池
     *
     * @param key  任务关联的缓存键
     * @param task 要执行的任务
     */
    private void submitTask(String key, Runnable task) {
        cancelTask(key);
        Future<?> future = executor.submit(task);
        activeTasks.put(key, future);

        // 超时监控
        executor.submit(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(TRANSACTION_TIMEOUT_SECONDS));
                cancelTask(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 取消指定任务
     *
     * @param key 任务关联的缓存键
     */
    private void cancelTask(String key) {
        Future<?> task = activeTasks.remove(key);
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * 对key进行MD5哈希处理
     *
     * @param key 原始键
     * @return 哈希后的键
     */
    private String hashKeyForDisk(String key) {
        String hashed = MD5.md5(key);
        return hashed.isEmpty() ? String.valueOf(key.hashCode()) : hashed;
    }

    /**
     * 基本类型与字节数组互转工具方法
     */

    /**
     * 将int类型转换为字节数组（4字节）
     * 采用大端字节序（高位字节在前）
     *
     * @param value 要转换的int值
     * @return 转换后的字节数组，长度为4
     */
    private byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    /**
     * 将字节数组（4字节）转换为int类型
     * 要求输入字节数组长度为4，采用大端字节序解析
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的int值
     */
    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
    }

    /**
     * 将long类型转换为字节数组（8字节）
     * 采用大端字节序（高位字节在前）
     *
     * @param value 要转换的long值
     * @return 转换后的字节数组，长度为8
     */
    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (value >>> (8 * (7 - i)));
        }
        return bytes;
    }

    /**
     * 将字节数组（8字节）转换为long类型
     * 要求输入字节数组长度为8，采用大端字节序解析
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的long值
     */
    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (bytes[i] & 0xFF)) << (8 * (7 - i));
        }
        return value;
    }

    /**
     * 将float类型转换为字节数组（4字节）
     * 先将float转换为对应的int位表示，再转换为字节数组
     *
     * @param value 要转换的float值
     * @return 转换后的字节数组，长度为4
     */
    private byte[] floatToBytes(float value) {
        return intToBytes(Float.floatToIntBits(value));
    }

    /**
     * 将字节数组（4字节）转换为float类型
     * 先将字节数组转换为int，再解析为对应的float值
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的float值
     */
    private float bytesToFloat(byte[] bytes) {
        return Float.intBitsToFloat(bytesToInt(bytes));
    }

    /**
     * 将boolean类型转换为字节数组（1字节）
     * true转换为1，false转换为0
     *
     * @param value 要转换的boolean值
     * @return 转换后的字节数组，长度为1
     */
    private byte[] booleanToBytes(boolean value) {
        return new byte[]{(byte) (value ? 1 : 0)};
    }

    /**
     * 将字节数组（1字节）转换为boolean类型
     * 字节值为1时返回true，其他值返回false
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的boolean值
     */
    private boolean bytesToBoolean(byte[] bytes) {
        return bytes[0] == 1;
    }

    /**
     * 通知带返回值的回调
     *
     * @param callback 回调接口
     * @param success  是否成功
     * @param result   结果数据
     * @param <T>      结果数据类型
     */
    private <T> void notifyBasicCallback(BasicCallback<T> callback, boolean success, T result) {
        if (callback == null) return;
        try {
            if (success) {
                callback.onSuccess(result);
            } else {
                callback.onFailure();
            }
        } catch (Exception e) {
            Log.e(TAG, "回调执行失败: " + e.getMessage());
        }
    }

    /**
     * 通知无返回值的回调
     *
     * @param callback 回调接口
     * @param success  是否成功
     */
    private void notifyEmptyCallback(EmptyCallback callback, boolean success) {
        if (callback == null) return;
        try {
            if (success) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        } catch (Exception e) {
            Log.e(TAG, "回调执行失败: " + e.getMessage());
        }
    }

    /**
     * 关闭缓存工具，释放资源
     */
    public void close() {
        try {
            // 取消所有任务
            for (Future<?> task : activeTasks.values()) {
                task.cancel(true);
            }
            activeTasks.clear();

            // 关闭线程池
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            // 关闭DiskLruCache
            if (diskCache != null && !diskCache.isClosed()) {
                diskCache.flush();
                diskCache.close();
                Log.i(TAG, "缓存工具已关闭");
            }
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "关闭缓存工具失败: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 带返回值的基础回调接口
     *
     * @param <T> 返回值类型
     */
    public interface BasicCallback<T> {
        /**
         * 操作成功时调用
         *
         * @param result 缓存值（put操作返回存入的值，get操作返回获取的值）
         */
        void onSuccess(T result);

        /**
         * 操作失败时调用
         */
        void onFailure();
    }

    /**
     * 无返回值的空回调接口（用于删除、清空等操作）
     */
    public interface EmptyCallback {
        /**
         * 操作成功时调用
         */
        void onSuccess();

        /**
         * 操作失败时调用
         */
        void onFailure();
    }
}