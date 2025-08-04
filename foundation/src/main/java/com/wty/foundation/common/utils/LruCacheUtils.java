package com.wty.foundation.common.utils;

import android.content.Context;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;
import com.wty.foundation.common.init.AppContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 磁盘LRU缓存工具类
 * 用于管理应用中的字符串和JSON数据缓存，基于DiskLruCache实现，提供线程安全的缓存增删改查、清空等功能
 * 采用单例模式，通过线程池管理IO操作，避免主线程阻塞
 */
public class LruCacheUtils {
    private static final String TAG = "LruCacheUtil"; // 日志标签
    private static final int APP_VERSION = 1; // 应用版本号，用于缓存版本控制
    private static final int VALUE_COUNT = 1; // 每个缓存项对应的value数量（DiskLruCache要求）
    private static final long MAX_CACHE_SIZE = 10 * 1024 * 1024; // 最大缓存容量：10MB
    private static final int IO_BUFFER_SIZE = 8 * 1024; // IO操作缓冲区大小：8KB
    private static final int TRANSACTION_TIMEOUT_SECONDS = 30; // 缓存操作超时时间：30秒

    private static volatile LruCacheUtils instance; // 单例实例，volatile保证多线程可见性
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 单线程池，保证IO操作串行执行
    private final ConcurrentHashMap<String, Future<?>> activeTasks = new ConcurrentHashMap<>(); // 管理活跃任务，避免重复操作
    private DiskLruCache diskCache; // DiskLruCache实例
    private final File cacheDir; // 缓存目录
    private final Context context; // 应用上下文

    /**
     * 私有构造方法，初始化缓存目录并初始化磁盘缓存
     *
     * @param context 应用上下文，用于获取缓存目录
     */
    private LruCacheUtils(Context context) {
        this.context = context.getApplicationContext(); // 使用应用上下文，避免内存泄漏
        this.cacheDir = getDiskCacheDir(this.context, "dc_cache"); // 获取缓存目录
        initDiskCache(); // 初始化磁盘缓存
    }

    /**
     * 获取单例实例
     *
     * @return LruCacheUtil单例
     */
    public static synchronized LruCacheUtils getInstance() {
        if (instance == null) {
            instance = new LruCacheUtils(AppContext.getInstance().getContext());
        }
        return instance;
    }

    /**
     * 初始化磁盘缓存
     * 若缓存目录不存在则创建，打开DiskLruCache实例
     */
    private void initDiskCache() {
        try {
            // 检查并创建缓存目录
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.w(TAG, "创建缓存目录失败: " + cacheDir.getAbsolutePath());
                return;
            }
            // 打开DiskLruCache
            diskCache = DiskLruCache.open(cacheDir, APP_VERSION, VALUE_COUNT, MAX_CACHE_SIZE);
            Log.i(TAG, "磁盘缓存初始化成功，目录: " + cacheDir.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "磁盘缓存初始化失败: " + e.getMessage());
        }
    }

    /**
     * 获取磁盘缓存目录
     * 优先使用外部存储缓存目录（卸载应用时自动删除），若不可用则使用内部缓存目录
     *
     * @param context    应用上下文
     * @param uniqueName 缓存目录唯一名称
     * @return 缓存目录文件
     */
    private File getDiskCacheDir(Context context, String uniqueName) {
        // 优先使用外部缓存目录
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null) {
            return new File(externalCacheDir, uniqueName);
        }
        // 外部目录不可用时，使用内部缓存目录
        return new File(context.getCacheDir(), uniqueName);
    }

    /**
     * 存入字符串缓存（无回调）
     *
     * @param key   缓存键
     * @param value 缓存值（字符串）
     */
    public void put(String key, String value) {
        put(key, value, null);
    }

    /**
     * 存入字符串缓存（带回调）
     * 会将key进行MD5哈希处理后存储，避免特殊字符问题
     *
     * @param key      缓存键
     * @param value    缓存值（字符串）
     * @param callback 操作结果回调（可为null）
     */
    public void put(String key, String value, CacheCallback callback) {
        // 校验参数合法性
        if (key == null || value == null) {
            Log.w(TAG, "key或value为null，无法存入缓存");
            notifyFailure(callback);
            return;
        }

        // 对key进行哈希处理
        final String hashedKey = hashKeyForDisk(key);
        // 提交任务到线程池执行
        submitTask(hashedKey, () -> {
            DiskLruCache.Editor editor = null;
            try {
                // 检查缓存是否初始化成功
                if (diskCache == null) {
                    notifyFailure(callback);
                    return;
                }

                // 获取编辑器（若已有编辑操作则返回null）
                editor = diskCache.edit(hashedKey);
                if (editor == null) {
                    Log.w(TAG, "获取缓存编辑器失败，可能已有相同key的编辑操作: " + hashedKey);
                    notifyFailure(callback);
                    return;
                }

                // 写入缓存值
                try (OutputStream os = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)) {
                    os.write(value.getBytes());
                }

                // 提交编辑并刷新缓存
                editor.commit();
                diskCache.flush();
                notifySuccess(callback, value);
                Log.d(TAG, "缓存写入成功: " + hashedKey);
            } catch (IOException e) {
                Log.e(TAG, "缓存写入失败: " + e.getMessage());
                // 发生异常时中止编辑
                if (editor != null) {
                    try {
                        editor.abort();
                    } catch (IOException abortEx) {
                        Log.e(TAG, "中止缓存编辑失败: " + abortEx.getMessage());
                    }
                }
                notifyFailure(callback);
            }
        });
    }

    /**
     * 同步获取字符串缓存
     * 直接在当前线程执行IO操作，建议在子线程调用
     *
     * @param key 缓存键
     * @return 缓存值（字符串），若不存在或发生错误则返回null
     */
    public String getSync(String key) {
        if (key == null) {
            Log.w(TAG, "key为null，无法获取缓存");
            return null;
        }

        final String hashedKey = hashKeyForDisk(key);
        DiskLruCache.Snapshot snapshot = null;
        try {
            // 检查缓存是否初始化成功
            if (diskCache == null) {
                return null;
            }

            // 获取缓存快照
            snapshot = diskCache.get(hashedKey);
            if (snapshot == null) {
                Log.d(TAG, "缓存不存在: " + hashedKey);
                return null;
            }

            // 读取缓存值
            try (InputStream is = snapshot.getInputStream(0); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[IO_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, bytesRead);
                }
                Log.d(TAG, "缓存读取成功: " + hashedKey);
                return sb.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "缓存读取失败: " + e.getMessage());
            return null;
        } finally {
            // 关闭快照（释放资源）
            if (snapshot != null) {
                snapshot.close();
            }
        }
    }

    /**
     * 异步获取字符串缓存
     * 结果通过回调返回，自动在子线程执行
     *
     * @param key      缓存键
     * @param callback 操作结果回调
     */
    public void get(String key, CacheCallback callback) {
        if (key == null) {
            Log.w(TAG, "key为null，无法获取缓存");
            notifyFailure(callback);
            return;
        }

        final String hashedKey = hashKeyForDisk(key);
        // 提交任务到线程池
        submitTask(hashedKey, () -> {
            String result = getSync(key);
            if (result != null) {
                notifySuccess(callback, result);
            } else {
                notifyFailure(callback);
            }
        });
    }

    /**
     * 存入JSON对象缓存（无回调）
     * 内部将JSON对象转为字符串存储
     *
     * @param key   缓存键
     * @param value 缓存值（JSON对象）
     */
    public void putJSON(String key, JSONObject value) {
        putJSON(key, value, null);
    }

    /**
     * 存入JSON对象缓存（带回调）
     * 内部将JSON对象转为字符串存储
     *
     * @param key      缓存键
     * @param value    缓存值（JSON对象）
     * @param callback 操作结果回调（可为null）
     */
    public void putJSON(String key, JSONObject value, CacheCallback callback) {
        if (value == null) {
            Log.w(TAG, "JSON对象为null，无法存入缓存");
            notifyFailure(callback);
            return;
        }
        // 转为字符串后调用put方法
        put(key, value.toString(), callback);
    }

    /**
     * 同步获取JSON对象缓存
     * 直接在当前线程执行，建议在子线程调用
     *
     * @param key 缓存键
     * @return JSON对象，若不存在、解析失败或发生错误则返回null
     */
    public JSONObject getJSONSync(String key) {
        String jsonString = getSync(key);
        if (jsonString == null) {
            return null;
        }

        // 解析字符串为JSON对象
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 异步获取JSON对象缓存
     * 结果通过回调返回，自动在子线程执行
     *
     * @param key      缓存键
     * @param callback JSON操作结果回调
     */
    public void getJSON(String key, JSONCallback callback) {
        if (key == null) {
            Log.w(TAG, "key为null，无法获取JSON缓存");
            if (callback != null) {
                callback.onFailure();
            }
            return;
        }

        final String hashedKey = hashKeyForDisk(key);
        // 提交任务到线程池
        submitTask(hashedKey, () -> {
            JSONObject result = getJSONSync(key);
            if (result != null) {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } else {
                if (callback != null) {
                    callback.onFailure();
                }
            }
        });
    }

    /**
     * 删除指定缓存（无回调）
     *
     * @param key 缓存键
     */
    public void remove(String key) {
        remove(key, null);
    }

    /**
     * 删除指定缓存（带回调）
     *
     * @param key      缓存键
     * @param callback 操作结果回调（可为null）
     */
    public void remove(String key, CacheCallback callback) {
        if (key == null) {
            Log.w(TAG, "key为null，无法删除缓存");
            notifyFailure(callback);
            return;
        }

        final String hashedKey = hashKeyForDisk(key);
        // 提交任务到线程池
        submitTask(hashedKey, () -> {
            try {
                if (diskCache == null) {
                    notifyFailure(callback);
                    return;
                }

                // 执行删除操作
                boolean success = diskCache.remove(hashedKey);
                if (success) {
                    diskCache.flush(); // 刷新缓存
                    notifySuccess(callback, null);
                    Log.d(TAG, "缓存删除成功: " + hashedKey);
                } else {
                    Log.w(TAG, "缓存删除失败，条目不存在: " + hashedKey);
                    notifyFailure(callback);
                }
            } catch (IOException e) {
                Log.e(TAG, "缓存删除异常: " + e.getMessage());
                notifyFailure(callback);
            }
        });
    }

    /**
     * 清空所有缓存
     * 会删除缓存目录并重新初始化，适用于需要完全清理缓存的场景
     *
     * @param callback 操作结果回调（可为null）
     */
    public void clearCache(CacheCallback callback) {
        submitTask("clear_cache", () -> {
            try {
                // 关闭并删除现有缓存
                if (diskCache != null) {
                    diskCache.delete();
                    diskCache.close();
                }

                // 重新初始化缓存
                initDiskCache();
                notifySuccess(callback, null);
                Log.i(TAG, "缓存已清空");
            } catch (IOException e) {
                Log.e(TAG, "清空缓存失败: " + e.getMessage());
                notifyFailure(callback);
            }
        });
    }

    /**
     * 获取当前缓存总大小
     *
     * @return 缓存大小（字节），若缓存未初始化则返回0
     */
    public long getCacheSize() {
        return diskCache != null ? diskCache.size() : 0;
    }

    /**
     * 刷新缓存到磁盘
     * 将内存中的缓存操作同步到磁盘，确保数据持久化
     */
    public void flushCache() {
        submitTask("flush_cache", () -> {
            try {
                if (diskCache != null) {
                    diskCache.flush();
                    Log.d(TAG, "缓存已刷新到磁盘");
                }
            } catch (IOException e) {
                Log.e(TAG, "刷新缓存失败: " + e.getMessage());
            }
        });
    }

    /**
     * 关闭缓存工具
     * 会取消所有未完成任务、关闭线程池并释放DiskLruCache资源
     * 建议在应用退出时调用
     */
    public void close() {
        try {
            // 取消所有活跃任务
            for (Future<?> task : activeTasks.values()) {
                task.cancel(true);
            }
            activeTasks.clear();

            // 关闭线程池
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // 强制关闭
            }

            // 关闭磁盘缓存
            if (diskCache != null && !diskCache.isClosed()) {
                diskCache.flush();
                diskCache.close();
                Log.i(TAG, "磁盘缓存已关闭");
            }
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "关闭缓存工具失败: " + e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
    }

    /**
     * 提交任务到线程池
     * 会先取消相同key的已有任务，避免重复执行
     *
     * @param key  任务关联的key（用于去重）
     * @param task 要执行的任务
     */
    private void submitTask(String key, Runnable task) {
        // 取消相同key的已有任务
        cancelTask(key);

        // 提交新任务并记录
        Future<?> future = executor.submit(task);
        activeTasks.put(key, future);

        // 监控任务超时，超时后自动取消
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
     * 取消指定key的任务
     *
     * @param key 任务关联的key
     */
    private void cancelTask(String key) {
        Future<?> task = activeTasks.remove(key);
        if (task != null) {
            task.cancel(true); // 取消任务（true表示可中断正在执行的任务）
        }
    }

    /**
     * 对key进行MD5哈希处理
     * 用于将可能包含特殊字符的key转为合法的文件名
     *
     * @param key 原始key
     * @return 哈希后的key（32位十六进制字符串）
     */
    private String hashKeyForDisk(String key) {
        // 计算哈希值
        String hashedKey = MD5.md5(key);
        // 极端情况：若MD5计算失败（如算法不支持），降级使用hashCode
        if (hashedKey.isEmpty()) {
            Log.w(TAG, "MD5计算失败，使用hashCode降级处理: " + key);
            hashedKey = String.valueOf(key.hashCode());
        }
        return hashedKey;
    }

    /**
     * 通知回调成功
     *
     * @param callback 回调接口
     * @param result   成功结果（字符串）
     */
    private void notifySuccess(CacheCallback callback, String result) {
        if (callback != null) {
            try {
                callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "成功回调执行失败: " + e.getMessage());
            }
        }
    }

    /**
     * 通知回调失败
     *
     * @param callback 回调接口
     */
    private void notifyFailure(CacheCallback callback) {
        if (callback != null) {
            try {
                callback.onFailure();
            } catch (Exception e) {
                Log.e(TAG, "失败回调执行异常: " + e.getMessage());
            }
        }
    }

    /**
     * 字符串缓存操作回调接口
     */
    public interface CacheCallback {
        /**
         * 操作成功时调用
         *
         * @param result 缓存值（put操作返回存入的值，get操作返回获取的值，remove/clear操作返回null）
         */
        void onSuccess(String result);

        /**
         * 操作失败时调用
         * 包括参数错误、IO异常、缓存未初始化等情况
         */
        void onFailure();
    }

    /**
     * JSON缓存操作回调接口
     */
    public interface JSONCallback {
        /**
         * 操作成功时调用
         *
         * @param result 解析后的JSON对象
         */
        void onSuccess(JSONObject result);

        /**
         * 操作失败时调用
         * 包括参数错误、IO异常、JSON解析失败等情况
         */
        void onFailure();
    }
}