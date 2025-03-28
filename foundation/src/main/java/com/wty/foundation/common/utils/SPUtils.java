package com.wty.foundation.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.wty.foundation.common.init.AppContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 用于操作 SharedPreferences 的工具类，提供了多种数据类型的存储和读取方法，
 * 还支持事务操作、数据备份恢复以及监听器管理等功能
 */
public class SPUtils {
    private static final String TAG = "SPUtils";
    // SharedPreferences 文件的名称
    private static final String PREF_NAME = "MyPrefsFile";
    // 存储普通数据的键前缀
    private static final String DATA_KEY_PREFIX = "data_";
    // 存储对象数据的键前缀
    private static final String OBJECT_KEY_PREFIX = "object_";
    // 存储 Bitmap 数据的键前缀
    private static final String BITMAP_KEY_PREFIX = "bitmap_";
    // 存储 Bitmap 的目录名称
    private static final String BITMAP_DIR_NAME = "bitmaps";
    // 临时恢复文件的名称
    private static final String TEMP_RESTORE_FILE = "sp_restore.tmp";
    private static final Gson GSON = new Gson();
    private static volatile SPUtils INSTANCE = null;
    // SharedPreferences 对象
    private SharedPreferences preferences;
    // SharedPreferences 的编辑器
    private SharedPreferences.Editor editor;
    // 存储事务操作的队列
    private final ConcurrentLinkedQueue<Runnable> transactionOperations = new ConcurrentLinkedQueue<>();
    // 存储事务回滚操作的队列
    private final ConcurrentLinkedQueue<Runnable> rollbackOperations = new ConcurrentLinkedQueue<>();
    // 存储原始值的 Map，用于事务回滚
    private final ConcurrentHashMap<String, Object> originalValues = new ConcurrentHashMap<>();
    // 用于 Bitmap 操作的读写锁
    private final ReentrantReadWriteLock bitmapLock = new ReentrantReadWriteLock();
    // 用于缓存 Bitmap 的 LruCache，使用 SoftReference 防止内存泄漏
    private final LruCache<String, SoftReference<Bitmap>> bitmapCache;
    // 存储 SharedPreferences 监听器的列表，使用 CopyOnWriteArrayList 保证线程安全
    private final CopyOnWriteArrayList<WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>> listeners = new CopyOnWriteArrayList<>();
    private final Object listenerLock = new Object();
    // 应用的上下文
    private final Context context;
    // 用于事务操作的锁对象
    private final Object transactionLock = new Object();
    // 事务操作的超时时间（秒）
    private static final int TRANSACTION_TIMEOUT_SECONDS = 30;
    // 默认的 Bitmap 压缩格式
    private static final CompressFormat DEFAULT_BITMAP_FORMAT = CompressFormat.PNG;
    // 默认的 Bitmap 压缩质量
    private static final int DEFAULT_BITMAP_QUALITY = 70;
    // 存储 Bitmap 的目录对象
    private final File bitmapDir;

    /**
     * 私有构造函数，初始化 SharedPreferences、编辑器、Bitmap 缓存和存储目录等
     */
    private SPUtils() {
        // 获取应用的上下文
        context = AppContext.getInstance().getContext();
        // 获取 SharedPreferences 实例
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // 获取 SharedPreferences 的编辑器
        editor = preferences.edit();

        // 计算 LruCache 的最大缓存大小，这里设置为应用最大内存的 1/8（以 KB 为单位）
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        // 初始化 LruCache
        bitmapCache = new LruCache<String, SoftReference<Bitmap>>(cacheSize) {
            /**
             * 计算缓存项的大小（以 KB 为单位），返回 Bitmap 的字节数除以 1024
             * 如果 Bitmap 为 null，则返回 0
             */
            @Override
            protected int sizeOf(String key, SoftReference<Bitmap> value) {
                Bitmap bitmap = value.get();
                return (bitmap != null) ? bitmap.getByteCount() / 1024 : 0;
            }

            /**
             * 当缓存项被移除时调用，回收被移除的 Bitmap（如果未被回收）
             */
            @Override
            protected void entryRemoved(boolean evicted, String key, SoftReference<Bitmap> oldValue, SoftReference<Bitmap> newValue) {
                Bitmap bitmap = oldValue.get();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        };

        // 创建存储 Bitmap 的目录对象
        bitmapDir = new File(context.getFilesDir(), BITMAP_DIR_NAME);
        // 如果目录不存在，则创建目录
        if (!bitmapDir.exists()) {
            bitmapDir.mkdirs();
        }
    }

    /**
     * 获取 SPUtils 的单例实例
     * 使用双重检查锁定确保线程安全
     *
     * @return SPUtils 的单例实例
     */
    public static SPUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (SPUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SPUtils();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 存储一个字符串值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putString(@NonNull String key, String value) {
        putString(key, value, false);
    }

    /**
     * 存储一个字符串值，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putString(@NonNull String key, String value, boolean useCommit) {
        editor.putString(DATA_KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字符串值，返回默认值 ""
     *
     * @param key 键
     * @return 存储的字符串值或默认值 ""
     */
    public String getString(@NonNull String key) {
        return getString(key, "");
    }

    /**
     * 获取一个字符串值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的字符串值或默认值
     */
    public String getString(@NonNull String key, String defaultValue) {
        return preferences.getString(DATA_KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个整数值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putInt(@NonNull String key, int value) {
        putInt(key, value, false);
    }

    /**
     * 存储一个整数值，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putInt(@NonNull String key, int value, boolean useCommit) {
        editor.putInt(DATA_KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个整数值，返回默认值 -1
     *
     * @param key 键
     * @return 存储的整数值或默认值 -1
     */
    public int getInt(@NonNull String key) {
        return getInt(key, -1);
    }

    /**
     * 获取一个整数值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的整数值或默认值
     */
    public int getInt(@NonNull String key, int defaultValue) {
        return preferences.getInt(DATA_KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个长整数值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putLong(@NonNull String key, long value) {
        putLong(key, value, false);
    }

    /**
     * 存储一个长整数值，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putLong(@NonNull String key, long value, boolean useCommit) {
        editor.putLong(DATA_KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个长整数值，返回默认值 -1L
     *
     * @param key 键
     * @return 存储的长整数值或默认值 -1L
     */
    public long getLong(@NonNull String key) {
        return getLong(key, -1L);
    }

    /**
     * 获取一个长整数值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的长整数值或默认值
     */
    public long getLong(@NonNull String key, long defaultValue) {
        return preferences.getLong(DATA_KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个浮点数值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putFloat(@NonNull String key, float value) {
        putFloat(key, value, false);
    }

    /**
     * 存储一个浮点数值，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putFloat(@NonNull String key, float value, boolean useCommit) {
        editor.putFloat(DATA_KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个浮点数值，返回默认值 -1f
     *
     * @param key 键
     * @return 存储的浮点数值或默认值 -1f
     */
    public float getFloat(@NonNull String key) {
        return getFloat(key, -1f);
    }

    /**
     * 获取一个浮点数值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的浮点数值或默认值
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        return preferences.getFloat(DATA_KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个双精度浮点数值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putDouble(@NonNull String key, double value) {
        putDouble(key, value, false);
    }

    /**
     * 存储一个双精度浮点数值，将其转换为长整数存储，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putDouble(@NonNull String key, double value, boolean useCommit) {
        putLong(key, Double.doubleToRawLongBits(value), useCommit);
    }

    /**
     * 获取一个双精度浮点数值，返回默认值 Double.NaN
     *
     * @param key 键
     * @return 存储的双精度浮点数值或默认值 Double.NaN
     */
    public double getDouble(@NonNull String key) {
        return getDouble(key, Double.NaN);
    }

    /**
     * 获取一个双精度浮点数值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的双精度浮点数值或默认值
     */
    public double getDouble(@NonNull String key, double defaultValue) {
        long bits = getLong(key, Double.doubleToRawLongBits(defaultValue));
        double value = Double.longBitsToDouble(bits);
        return Double.isNaN(value) ? defaultValue : value;
    }

    /**
     * 存储一个布尔值，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putBoolean(@NonNull String key, boolean value) {
        putBoolean(key, value, false);
    }

    /**
     * 存储一个布尔值，可以选择提交方式（commit 或 apply）
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putBoolean(@NonNull String key, boolean value, boolean useCommit) {
        editor.putBoolean(DATA_KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个布尔值，返回默认值 false
     *
     * @param key 键
     * @return 存储的布尔值或默认值 false
     */
    public boolean getBoolean(@NonNull String key) {
        return getBoolean(key, false);
    }

    /**
     * 获取一个布尔值，返回指定的默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的布尔值或默认值
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return preferences.getBoolean(DATA_KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个字符串集合，使用默认的提交方式（apply）
     *
     * @param key   键
     * @param value 值
     */
    public void putStringSet(@NonNull String key, Set<String> value) {
        putStringSet(key, value, false);
    }

    /**
     * 存储一个字符串集合，可以选择提交方式（commit 或 apply）
     * 对传入的集合进行安全处理，防止传入 null
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putStringSet(@NonNull String key, Set<String> value, boolean useCommit) {
        Set<String> safeSet = (value != null) ? new HashSet<>(value) : new HashSet<>();
        editor.putStringSet(DATA_KEY_PREFIX + key, safeSet);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字符串集合，返回默认的空集合
     *
     * @param key 键
     * @return 存储的字符串集合或默认的空集合
     */
    public Set<String> getStringSet(@NonNull String key) {
        return getStringSet(key, Collections.emptySet());
    }

    /**
     * 获取一个字符串集合，返回指定的默认集合
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的字符串集合或默认值
     */
    public Set<String> getStringSet(@NonNull String key, Set<String> defaultValue) {
        Set<String> set = preferences.getStringSet(DATA_KEY_PREFIX + key, null);
        return set != null ? new HashSet<>(set) : defaultValue;
    }

    /**
     * 存储可序列化对象，使用默认的提交方式（apply）
     *
     * @param <T> 可序列化对象的类型，必须实现 Serializable 接口
     * @param key 存储对象的键
     * @param obj 要存储的可序列化对象
     */
    public <T extends Serializable> void putSerializableObject(@NonNull String key, T obj) {
        putSerializableObject(key, obj, false);
    }

    /**
     * 存储可序列化对象，可以选择提交方式（commit 或 apply）
     * 将对象转换为 JSON 字符串并存储到 SharedPreferences 中
     *
     * @param <T>       可序列化对象的类型，必须实现 Serializable 接口
     * @param key       存储对象的键
     * @param obj       要存储的可序列化对象
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <T extends Serializable> void putSerializableObject(@NonNull String key, T obj, boolean useCommit) {
        try {
            // 将对象转换为 JSON 字符串并存储
            editor.putString(OBJECT_KEY_PREFIX + key, GSON.toJson(obj));
            commitOrApply(useCommit);
        } catch (Exception e) {
            // 若序列化过程中出现异常，记录错误日志
            Log.e(TAG, "Failed to serialize object, key: " + key, e);
        }
    }

    /**
     * 获取可序列化对象
     * 从 SharedPreferences 中读取 JSON 字符串并将其转换为对象
     *
     * @param <T>           可序列化对象的类型，必须实现 Serializable 接口
     * @param key           存储对象的键
     * @param clazz         对象的类类型
     * @param defaultObject 如果未找到对应的值，返回的默认对象
     * @return 存储的可序列化对象或默认对象
     */
    public <T extends Serializable> T getSerializableObject(@NonNull String key, Class<T> clazz, @Nullable T defaultObject) {
        // 从 SharedPreferences 中获取存储的 JSON 字符串
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) return defaultObject;
        try {
            // 将 JSON 字符串转换为对象
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            // 若反序列化过程中出现异常，记录错误日志并返回默认对象
            Log.e(TAG, "Failed to deserialize object, key: " + key, e);
            return defaultObject;
        }
    }

    /**
     * 存储可打包对象，使用默认的提交方式（apply）
     *
     * @param <T> 可打包对象的类型，必须实现 Parcelable 接口
     * @param key 存储对象的键
     * @param obj 要存储的可打包对象
     */
    public <T extends Parcelable> void putParcelableObject(@NonNull String key, T obj) {
        putParcelableObject(key, obj, false);
    }

    /**
     * 存储可打包对象，可以选择提交方式（commit 或 apply）
     * 将对象写入 Parcel 并转换为字节数组存储
     *
     * @param <T>       可打包对象的类型，必须实现 Parcelable 接口
     * @param key       存储对象的键
     * @param obj       要存储的可打包对象
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <T extends Parcelable> void putParcelableObject(@NonNull String key, T obj, boolean useCommit) {
        // 获取一个 Parcel 实例
        Parcel parcel = Parcel.obtain();
        try {
            // 将对象写入 Parcel
            parcel.writeParcelable(obj, 0);
            // 将 Parcel 内容转换为字节数组
            byte[] bytes = parcel.marshall();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android S 及以上版本的处理方式
                parcel.writeByteArray(bytes);
            } else {
                // 旧版本将字节数组存储到 SharedPreferences
                putBytes(OBJECT_KEY_PREFIX + key, bytes, useCommit);
            }
        } finally {
            // 回收 Parcel 实例
            parcel.recycle();
        }
    }

    /**
     * 获取可打包对象
     * 从 SharedPreferences 中读取字节数组并将其转换为对象
     *
     * @param <T>           可打包对象的类型，必须实现 Parcelable 接口
     * @param key           存储对象的键
     * @param clazz         对象的类类型
     * @param defaultObject 如果未找到对应的值，返回的默认对象
     * @return 存储的可打包对象或默认对象
     */
    public <T extends Parcelable> T getParcelableObject(@NonNull String key, Class<T> clazz, @Nullable T defaultObject) {
        // 从 SharedPreferences 中获取存储的字节数组
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) return defaultObject;

        // 获取一个 Parcel 实例
        Parcel parcel = Parcel.obtain();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android S 及以上版本的处理方式
                bytes = parcel.createByteArray();
            }
            // 将字节数组写入 Parcel
            parcel.unmarshall(bytes, 0, bytes.length);
            // 设置 Parcel 的数据位置为起始位置
            parcel.setDataPosition(0);
            // 从 Parcel 中读取对象
            return parcel.readParcelable(clazz.getClassLoader());
        } catch (Exception e) {
            // 若反序列化过程中出现异常，记录错误日志并返回默认对象
            Log.e(TAG, "Failed to deserialize Parcelable object, key: " + key, e);
            return defaultObject;
        } finally {
            // 回收 Parcel 实例
            parcel.recycle();
        }
    }

    /**
     * 存储可序列化对象列表，使用默认的提交方式（apply）
     *
     * @param <T>  可序列化对象的类型，必须实现 Serializable 接口
     * @param key  存储列表的键
     * @param list 要存储的可序列化对象列表
     */
    public <T extends Serializable> void putSerializableList(@NonNull String key, List<T> list) {
        putSerializableList(key, list, false);
    }

    /**
     * 存储可序列化对象列表，可以选择提交方式（commit 或 apply）
     * 将列表转换为 JSON 字符串并存储到 SharedPreferences 中
     *
     * @param <T>       可序列化对象的类型，必须实现 Serializable 接口
     * @param key       存储列表的键
     * @param list      要存储的可序列化对象列表
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <T extends Serializable> void putSerializableList(@NonNull String key, List<T> list, boolean useCommit) {
        try {
            // 将列表转换为 JSON 字符串并存储
            editor.putString(OBJECT_KEY_PREFIX + key, GSON.toJson(list));
            commitOrApply(useCommit);
        } catch (Exception e) {
            // 若序列化过程中出现异常，记录错误日志
            Log.e(TAG, "Failed to store Serializable list, key: " + key, e);
        }
    }

    /**
     * 获取可序列化对象列表
     * 从 SharedPreferences 中读取 JSON 字符串并将其转换为列表
     *
     * @param <T>   可序列化对象的类型，必须实现 Serializable 接口
     * @param key   存储列表的键
     * @param clazz 对象的类类型
     * @return 存储的可序列化对象列表或空列表
     */
    public <T extends Serializable> List<T> getSerializableList(@NonNull String key, Class<T> clazz) {
        // 从 SharedPreferences 中获取存储的 JSON 字符串
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) return new ArrayList<>();

        try {
            // 获取列表的类型
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            // 将 JSON 字符串转换为列表
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            // 若反序列化过程中出现异常，记录错误日志并返回空列表
            Log.e(TAG, "Failed to get Serializable list, key: " + key, e);
            return new ArrayList<>();
        }
    }

    /**
     * 存储可打包对象列表，使用默认的提交方式（apply）
     *
     * @param <T>  可打包对象的类型，必须实现 Parcelable 接口
     * @param key  存储列表的键
     * @param list 要存储的可打包对象列表
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list) {
        putParcelableList(key, list, false);
    }

    /**
     * 存储可打包对象列表，可以选择提交方式（commit 或 apply）
     * 将列表写入 Parcel 并转换为字节数组存储
     *
     * @param <T>       可打包对象的类型，必须实现 Parcelable 接口
     * @param key       存储列表的键
     * @param list      要存储的可打包对象列表
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list, boolean useCommit) {
        if (list == null) {
            // 如果列表为空，从 SharedPreferences 中移除该键
            editor.remove(OBJECT_KEY_PREFIX + key);
            commitOrApply(useCommit);
            return;
        }

        // 获取一个 Parcel 实例
        Parcel parcel = Parcel.obtain();
        try {
            // 将列表写入 Parcel
            parcel.writeList(list);
            // 将 Parcel 内容转换为字节数组
            byte[] bytes = parcel.marshall();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android S 及以上版本的处理方式
                parcel.writeByteArray(bytes);
            } else {
                // 旧版本将字节数组存储到 SharedPreferences
                putBytes(OBJECT_KEY_PREFIX + key, bytes, useCommit);
            }
        } finally {
            // 回收 Parcel 实例
            parcel.recycle();
        }
    }

    /**
     * 获取可打包对象列表
     * 从 SharedPreferences 中读取字节数组并将其转换为列表
     *
     * @param <T>   可打包对象的类型，必须实现 Parcelable 接口
     * @param key   存储列表的键
     * @param clazz 对象的类类型
     * @return 存储的可打包对象列表或空列表
     */
    public <T extends Parcelable> List<T> getParcelableList(@NonNull String key, Class<T> clazz) {
        // 从 SharedPreferences 中获取存储的字节数组
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) return new ArrayList<>();

        // 获取一个 Parcel 实例
        Parcel parcel = Parcel.obtain();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android S 及以上版本的处理方式
                bytes = parcel.createByteArray();
            }
            // 将字节数组写入 Parcel
            parcel.unmarshall(bytes, 0, bytes.length);
            // 设置 Parcel 的数据位置为起始位置
            parcel.setDataPosition(0);
            // 从 Parcel 中读取原始列表
            List<?> rawList = parcel.readArrayList(clazz.getClassLoader());

            List<T> result = new ArrayList<>();
            if (rawList != null) {
                for (Object item : rawList) {
                    if (clazz.isInstance(item)) {
                        // 将符合类型的对象添加到结果列表中
                        result.add(clazz.cast(item));
                    } else {
                        // 若对象类型不匹配，记录错误日志
                        Log.e(TAG, "Invalid item type in Parcelable list, key: " + key + ", expected " + clazz.getName() + ", but got " + item.getClass().getName());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            // 若反序列化过程中出现异常，记录错误日志并返回空列表
            Log.e(TAG, "Failed to get Parcelable list, key: " + key, e);
            return new ArrayList<>();
        } finally {
            // 回收 Parcel 实例
            parcel.recycle();
        }
    }

    /**
     * 存储 Map 对象，使用默认的提交方式（apply）
     *
     * @param <K> 键的类型
     * @param <V> 值的类型
     * @param key 存储 Map 的键
     * @param map 要存储的 Map 对象
     */
    public <K, V> void putMap(@NonNull String key, Map<K, V> map) {
        putMap(key, map, false);
    }

    /**
     * 存储 Map 对象，可以选择提交方式（commit 或 apply）
     * 将 Map 转换为 JSON 字符串并存储到 SharedPreferences 中
     *
     * @param <K>       键的类型
     * @param <V>       值的类型
     * @param key       存储 Map 的键
     * @param map       要存储的 Map 对象
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <K, V> void putMap(@NonNull String key, Map<K, V> map, boolean useCommit) {
        try {
            // 将 Map 转换为 JSON 字符串并存储
            editor.putString(OBJECT_KEY_PREFIX + key, GSON.toJson(map));
            commitOrApply(useCommit);
        } catch (Exception e) {
            // 若序列化过程中出现异常，记录错误日志
            Log.e(TAG, "Failed to store Map, key: " + key);
        }
    }

    /**
     * 获取 Map 对象
     * 从 SharedPreferences 中读取 JSON 字符串并将其转换为 Map
     *
     * @param <K>        键的类型
     * @param <V>        值的类型
     * @param key        存储 Map 的键
     * @param keyClass   键的类类型
     * @param valueClass 值的类类型
     * @return 存储的 Map 对象或空 Map
     */
    public <K, V> Map<K, V> getMap(@NonNull String key, Class<K> keyClass, Class<V> valueClass) {
        // 从 SharedPreferences 中获取存储的 JSON 字符串
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) return new HashMap<>();

        try {
            // 获取 Map 的类型
            Type type = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
            // 将 JSON 字符串转换为 Map
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            // 若反序列化过程中出现异常，记录错误日志并返回空 Map
            Log.e(TAG, "Failed to get Map, key: " + key);
            return new HashMap<>();
        }
    }

    /**
     * 存储 Map 列表，使用默认的提交方式（apply）
     *
     * @param <K>  键的类型
     * @param <V>  值的类型
     * @param key  存储 Map 列表的键
     * @param list 要存储的 Map 列表
     */
    public <K, V> void putMapList(@NonNull String key, List<Map<K, V>> list) {
        putMapList(key, list, false);
    }

    /**
     * 存储 Map 列表，可以选择提交方式（commit 或 apply）
     * 将 Map 列表转换为 JSON 字符串并存储到 SharedPreferences 中
     *
     * @param <K>       键的类型
     * @param <V>       值的类型
     * @param key       存储 Map 列表的键
     * @param list      要存储的 Map 列表
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public <K, V> void putMapList(@NonNull String key, List<Map<K, V>> list, boolean useCommit) {
        try {
            // 将 Map 列表转换为 JSON 字符串并存储
            editor.putString(OBJECT_KEY_PREFIX + key, GSON.toJson(list));
            commitOrApply(useCommit);
        } catch (Exception e) {
            // 若序列化过程中出现异常，记录错误日志
            Log.e(TAG, "Failed to store Map list, key: " + key);
        }
    }

    /**
     * 获取 Map 列表
     * 从 SharedPreferences 中读取 JSON 字符串并将其转换为 Map 列表
     *
     * @param <K>        键的类型
     * @param <V>        值的类型
     * @param key        存储 Map 列表的键
     * @param keyClass   键的类类型
     * @param valueClass 值的类类型
     * @return 存储的 Map 列表或空列表
     */
    public <K, V> List<Map<K, V>> getMapList(@NonNull String key, Class<K> keyClass, Class<V> valueClass) {
        // 从 SharedPreferences 中获取存储的 JSON 字符串
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) return new ArrayList<>();

        try {
            // 获取 Map 列表的类型
            Type type = TypeToken.getParameterized(List.class, TypeToken.getParameterized(Map.class, keyClass, valueClass).getType()).getType();
            // 将 JSON 字符串转换为 Map 列表
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            // 若反序列化过程中出现异常，记录错误日志并返回空列表
            Log.e(TAG, "Failed to get Map list, key: " + key);
            return new ArrayList<>();
        }
    }

    /**
     * 存储 Bitmap，使用默认的压缩格式和质量，以及默认的提交方式（apply）
     *
     * @param key    存储 Bitmap 的键
     * @param bitmap 要存储的 Bitmap 对象
     */
    public void putBitmap(@NonNull String key, @NonNull Bitmap bitmap) {
        putBitmap(key, bitmap, DEFAULT_BITMAP_FORMAT, DEFAULT_BITMAP_QUALITY, false);
    }

    /**
     * 存储 Bitmap，可以指定压缩格式、质量和提交方式
     * 将 Bitmap 压缩后保存为文件，并将文件路径存储到 SharedPreferences 中，同时更新缓存
     *
     * @param key       存储 Bitmap 的键
     * @param bitmap    要存储的 Bitmap 对象
     * @param format    压缩格式
     * @param quality   压缩质量（0-100）
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putBitmap(@NonNull String key, @NonNull Bitmap bitmap, CompressFormat format, int quality, boolean useCommit) {
        if (bitmap == null) {
            // 如果 Bitmap 为 null，记录错误日志并返回
            Log.e(TAG, "Attempting to store null Bitmap, key: " + key);
            return;
        }

        if (bitmap.isRecycled()) {
            // 如果 Bitmap 已被回收，记录错误日志并返回
            Log.e(TAG, "Attempting to store a recycled Bitmap, key: " + key);
            return;
        }

        // 生成唯一的文件名
        String fileName = UUID.randomUUID().toString() + "." + format.name().toLowerCase();
        // 创建文件对象
        File newFile = new File(bitmapDir, fileName);
        // 获取文件的绝对路径
        String newFilePath = newFile.getAbsolutePath();

        // 获取写锁，确保同一时间只有一个线程可以写入
        bitmapLock.writeLock().lock();
        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            // 压缩 Bitmap 并写入文件
            if (bitmap.compress(format, Math.max(0, Math.min(quality, 100)), fos)) {
                synchronized (transactionLock) {
                    // 将文件路径存储到 SharedPreferences
                    editor.putString(BITMAP_KEY_PREFIX + key, newFilePath);
                    // 提交更改
                    commitOrApply(useCommit);
                    // 将 Bitmap 缓存到 LruCache 中，使用 SoftReference 防止内存泄漏
                    bitmapCache.put(key, new SoftReference<>(bitmap));
                }

                // 获取之前存储的文件路径
                String oldFilePath = preferences.getString(BITMAP_KEY_PREFIX + key, null);
                if (oldFilePath != null && !oldFilePath.equals(newFilePath)) {
                    // 如果存在旧文件路径且与新路径不同，启动新线程删除旧文件
                    new Thread(() -> deleteOldFileWithRetry(oldFilePath)).start();
                }

            } else {
                // 如果压缩失败，记录错误日志
                Log.e(TAG, "Bitmap compression failed, key: " + key);
                if (newFile.exists() && !newFile.delete()) {
                    // 如果无法删除损坏的文件，记录警告日志
                    Log.w(TAG, "Could not delete corrupted image file: " + newFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            // 如果保存图像时出现异常，记录错误日志
            Log.e(TAG, "Failed to save image, key: " + key, e);
            if (newFile.exists() && !newFile.delete()) {
                // 如果无法删除损坏的文件，记录警告日志
                Log.w(TAG, "Could not delete corrupted image file: " + newFile.getAbsolutePath());
            }
        } finally {
            // 释放写锁
            bitmapLock.writeLock().unlock();
        }
    }

    /**
     * 获取 Bitmap
     * 先从缓存中获取，如果缓存中没有则从文件中读取并更新缓存
     *
     * @param key           存储 Bitmap 的键
     * @param defaultBitmap 如果未找到对应 Bitmap，返回的默认 Bitmap 对象
     * @return 存储的 Bitmap 对象或默认 Bitmap
     */
    public Bitmap getBitmap(@NonNull String key, @Nullable Bitmap defaultBitmap) {
        // 获取读锁，允许多个线程同时读取
        bitmapLock.readLock().lock();
        try {
            // 从缓存中获取 Bitmap 的 SoftReference
            SoftReference<Bitmap> cachedReference = bitmapCache.get(key);
            // 获取缓存中的 Bitmap
            Bitmap cached = (cachedReference != null) ? cachedReference.get() : null;

            if (cached == null) {
                // 如果缓存中没有，从 SharedPreferences 中获取文件路径
                String path = preferences.getString(BITMAP_KEY_PREFIX + key, null);
                if (path != null) {
                    // 创建文件对象
                    File file = new File(path);
                    if (file.exists()) {
                        try {
                            // 先只获取图像的边界信息，不实际解码图像
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(path, options);
                            if (options.outWidth <= 0 || options.outHeight <= 0) {
                                // 如果图像文件损坏，记录错误日志并移除该 Bitmap 的记录
                                Log.e(TAG, "Corrupted image file: " + path);
                                removeBitmap(key, true);
                                return defaultBitmap;
                            }

                            // 实际解码图像
                            options.inJustDecodeBounds = false;
                            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                            if (bitmap != null) {
                                // 将解码后的 Bitmap 缓存到 LruCache 中
                                bitmapCache.put(key, new SoftReference<>(bitmap));
                                return bitmap;
                            }
                        } catch (Exception e) {
                            // 如果加载图像时出现异常，记录错误日志
                            Log.e(TAG, "Failed to load image, key: " + key, e);
                        }
                    } else {
                        // 如果文件不存在，移除该 Bitmap 的记录
                        removeBitmap(key, true);
                    }
                }
                return defaultBitmap;
            }

            if (cached.isRecycled()) {
                // 如果缓存中的 Bitmap 已被回收，从缓存中移除
                bitmapCache.remove(key);
                return defaultBitmap;
            }

            return cached;
        } finally {
            // 释放读锁
            bitmapLock.readLock().unlock();
        }
    }

    /**
     * 移除 Bitmap
     * 删除对应的文件，从 SharedPreferences 中移除路径记录，并从缓存中移除
     *
     * @param key       存储 Bitmap 的键
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void removeBitmap(@NonNull String key, boolean useCommit) {
        // 从 SharedPreferences 中获取文件路径
        String path = preferences.getString(BITMAP_KEY_PREFIX + key, null);
        if (path != null) {
            // 删除文件
            new File(path).delete();
            // 从 SharedPreferences 中移除路径记录
            editor.remove(BITMAP_KEY_PREFIX + key);
            // 提交更改
            commitOrApply(useCommit);
            // 从缓存中移除 Bitmap
            bitmapCache.remove(key);
        }
    }

    /**
     * 存储字节数组，使用默认的提交方式（apply）
     *
     * @param key   存储字节数组的键
     * @param value 要存储的字节数组
     */
    public void putBytes(@NonNull String key, byte[] value) {
        putBytes(key, value, false);
    }

    /**
     * 存储字节数组，可以选择提交方式（commit 或 apply）
     * 将字节数组编码为 Base64 字符串并存储到 SharedPreferences 中
     *
     * @param key       存储字节数组的键
     * @param value     要存储的字节数组
     * @param useCommit 如果为 true，则使用 commit 提交更改；否则使用 apply
     */
    public void putBytes(@NonNull String key, byte[] value, boolean useCommit) {
        // 将字节数组编码为 Base64 字符串并存储
        editor.putString(OBJECT_KEY_PREFIX + key, Base64.encodeToString(value, Base64.NO_WRAP));
        commitOrApply(useCommit);
    }

    /**
     * 获取字节数组
     * 从 SharedPreferences 中读取 Base64 编码的字符串并解码为字节数组
     *
     * @param key          存储字节数组的键
     * @param defaultValue 如果未找到对应的值，返回的默认字节数组
     * @return 存储的字节数组或默认字节数组
     */
    public byte[] getBytes(@NonNull String key, @Nullable byte[] defaultValue) {
        // 从 SharedPreferences 中获取存储的 Base64 编码字符串
        String encoded = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (encoded == null) return defaultValue;
        try {
            // 解码 Base64 字符串为字节数组
            return Base64.decode(encoded, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            // 如果解码失败，记录错误日志并返回默认字节数组
            Log.e(TAG, "Base64 decoding failed: " + key, e);
            return defaultValue;
        }
    }

    /**
     * 获取 SharedPreferences 中所有的键值对
     *
     * @return 包含所有键值对的 Map
     */
    public Map<String, Object> getAllEntries() {
        // 获取 SharedPreferences 中所有的键值对
        Map<String, ?> all = preferences.getAll();
        // 创建一个 LinkedHashMap 用于存储结果
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            // 将键值对添加到结果 Map 中
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 开始一个事务
     * 清空事务操作队列、回滚操作队列和原始值 Map
     */
    public void beginTransaction() {
        synchronized (transactionLock) {
            transactionOperations.clear();
            rollbackOperations.clear();
            originalValues.clear();
        }
    }

    /**
     * 向事务中添加一个操作和对应的回滚操作
     *
     * @param operation 要执行的操作
     * @param key       操作对应的键（用于存储原始值）
     * @param rollback  操作失败时的回滚操作
     */
    public void addToTransaction(Runnable operation, String key, Runnable rollback) {
        if (operation == null) return;
        synchronized (transactionLock) {
            if (key != null && preferences.contains(key)) {
                // 如果键存在，存储其原始值
                originalValues.put(key, preferences.getAll().get(key));
            }
            // 添加操作到事务操作队列
            transactionOperations.add(operation);
            if (rollback != null) {
                // 如果存在回滚操作，添加到回滚操作队列
                rollbackOperations.add(rollback);
            }
        }
    }

    /**
     * 提交事务
     * 执行事务操作队列中的所有操作，如果出现异常或超时则回滚事务
     *
     * @return 如果事务成功提交返回 true，否则返回 false
     */
    public boolean commitTransaction() {
        synchronized (transactionLock) {
            long startTime = System.currentTimeMillis();
            boolean success = true;
            Iterator<Runnable> iterator = transactionOperations.iterator();
            while (iterator.hasNext()) {
                try {
                    // 获取并执行下一个操作
                    Runnable operation = iterator.next();
                    operation.run();
                    iterator.remove();
                } catch (Exception e) {
                    // 如果操作执行失败，设置 success 为 false 并记录错误日志
                    success = false;
                    Log.e(TAG, "Transaction execution failed: " + e.getMessage(), e);
                    break;
                }

                if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(TRANSACTION_TIMEOUT_SECONDS)) {
                    // 如果事务执行超时，设置 success 为 false 并记录错误日志
                    success = false;
                    Log.e(TAG, "Transaction timed out");
                    break;
                }
            }

            if (!success) {
                // 如果事务失败，回滚事务
                rollbackTransaction();
            } else {
                // 如果事务成功，提交 SharedPreferences 的更改
                editor.apply();
            }
            // 清空事务操作队列和回滚操作队列
            transactionOperations.clear();
            rollbackOperations.clear();
            return success;
        }
    }

    /**
     * 回滚事务
     * 执行回滚操作队列中的所有操作，并恢复原始值
     */
    private void rollbackTransaction() {
        synchronized (transactionLock) {
            Iterator<Runnable> rollbackIterator = rollbackOperations.iterator();
            while (rollbackIterator.hasNext()) {
                // 获取并执行下一个回滚操作
                Runnable rollback = rollbackIterator.next();
                try {
                    rollback.run();
                } catch (Exception e) {
                    // 如果回滚操作执行失败，记录错误日志
                    Log.e(TAG, "Rollback operation execution failed: " + e.getMessage(), e);
                } finally {
                    rollbackIterator.remove();
                }
            }

            for (Map.Entry<String, Object> entry : originalValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    // 如果原始值为 null，从 SharedPreferences 中移除该键
                    editor.remove(key);
                } else if (value instanceof String) {
                    // 如果原始值为字符串，恢复字符串值
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    // 如果原始值为整数，恢复整数值
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    // 如果原始值为长整数，恢复长整数值
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    // 如果原始值为浮点数，恢复浮点数值
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof Boolean) {
                    // 如果原始值为布尔值，恢复布尔值
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Set<?>) {
                    try {
                        // 如果原始值为字符串集合，恢复字符串集合值
                        @SuppressWarnings("unchecked") Set<String> stringSet = (Set<String>) value;
                        editor.putStringSet(key, stringSet);
                    } catch (ClassCastException e) {
                        // 如果类型转换失败，记录警告日志
                        Log.w(TAG, "Type conversion failed, skipping rollback key: " + key);
                    }
                }
            }
            // 提交 SharedPreferences 的更改
            editor.apply();
            // 清空原始值 Map
            originalValues.clear();
        }
    }

    /**
     * 注册 SharedPreferences 的监听器
     *
     * @param listener 要注册的监听器
     */
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener == null) return;
        synchronized (listenerLock) {
            // 将监听器的弱引用添加到监听器列表中
            listeners.add(new WeakReference<>(listener));
            // 注册监听器到 SharedPreferences
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * 注销 SharedPreferences 的监听器
     *
     * @param listener 要注销的监听器
     */
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener == null) return;
        synchronized (listenerLock) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 从监听器列表中移除对应的监听器弱引用
                listeners.removeIf(ref -> {
                    SharedPreferences.OnSharedPreferenceChangeListener l = ref.get();
                    if (l == null || l == listener) {
                        if (l != null) {
                            // 从 SharedPreferences 中注销监听器
                            preferences.unregisterOnSharedPreferenceChangeListener(l);
                        }
                        return true;
                    }
                    return false;
                });
            } else {
                // 对于低于N的API级别，请手动迭代和删除
                Iterator<WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>> iterator = listeners.iterator();
                while (iterator.hasNext()) {
                    WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> ref = iterator.next();
                    SharedPreferences.OnSharedPreferenceChangeListener l = ref.get();
                    if (l == null || l == listener) {
                        if (l != null) {
                            // 从 SharedPreferences 中注销监听器
                            preferences.unregisterOnSharedPreferenceChangeListener(l);
                        }
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * 备份 SharedPreferences 文件
     *
     * @param backupFile 备份文件的 File 对象
     * @return 如果备份成功返回 true，否则返回 false
     */
    public synchronized boolean backupSharedPreferences(File backupFile) {
        // 获取 SharedPreferences 文件对象
        File prefFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");
        if (!prefFile.exists()) return false;

        try (InputStream in = new FileInputStream(prefFile); OutputStream out = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                // 将文件内容写入备份文件
                out.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            // 如果备份失败，记录错误日志
            Log.e(TAG, "Backup failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 恢复 SharedPreferences 文件，从指定的备份文件中恢复数据
     * 此方法会将备份文件内容复制到临时文件，验证临时文件的有效性，
     * 若有效则删除原配置文件并将临时文件重命名为原配置文件，最后更新相关对象状态
     *
     * @param backupFile 用于恢复的备份文件对象
     * @return 若恢复成功返回 true，否则返回 false
     */
    public synchronized boolean restoreSharedPreferences(File backupFile) {
        // 检查备份文件是否存在，若不存在则直接返回 false，并记录详细日志
        if (!backupFile.exists()) {
            Log.e(TAG, "Backup file for restoring SharedPreferences does not exist. File path: " + backupFile.getAbsolutePath());
            return false;
        }

        // 创建临时文件对象，用于存储备份文件内容
        File tempFile = new File(context.getFilesDir(), TEMP_RESTORE_FILE);
        // 获取原 SharedPreferences 文件对象
        File prefFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");

        try (
                // 创建输入流，用于读取备份文件内容
                InputStream in = new FileInputStream(backupFile);
                // 创建输出流，用于将备份文件内容写入临时文件
                OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            // 循环读取备份文件内容并写入临时文件
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            // 验证临时文件是否为有效的 SharedPreferences 文件
            if (!isValidSharedPreferences(tempFile)) {
                // 若无效，记录详细错误日志并返回 false
                Log.e(TAG, "The temporary file created from the backup file is not a valid SharedPreferences file. File path: " + tempFile.getAbsolutePath());
                return false;
            }

            // 加锁以保证在恢复过程中不会有其他操作干扰
            synchronized (transactionLock) {
                // 若原配置文件存在且无法删除，记录详细错误日志并返回 false
                if (prefFile.exists() && !prefFile.delete()) {
                    Log.e(TAG, "Failed to delete the old SharedPreferences configuration file. File path: " + prefFile.getAbsolutePath());
                    return false;
                }
                // 尝试将临时文件重命名为原配置文件，若失败则记录详细错误日志并返回 false
                if (!tempFile.renameTo(prefFile)) {
                    Log.e(TAG, "Failed to replace the old SharedPreferences file with the temporary file. Temporary file path: " + tempFile.getAbsolutePath() + ", Old file path: " + prefFile.getAbsolutePath());
                    return false;
                }

                // 获取新的 SharedPreferences 实例
                SharedPreferences newPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                // 加锁更新当前的 preferences 和 editor 对象
                synchronized (this) {
                    preferences = newPrefs;
                    editor = newPrefs.edit();
                }

                // 清空 Bitmap 缓存
                bitmapCache.evictAll();
                // 清空事务操作列表
                transactionOperations.clear();
            }
            // 恢复成功，返回 true
            return true;
        } catch (IOException e) {
            // 若恢复过程中出现异常，记录详细错误日志
            Log.e(TAG, "An I/O exception occurred during the restoration of SharedPreferences. Message: " + e.getMessage(), e);
            // 若临时文件存在且无法删除，记录详细警告日志
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w(TAG, "Failed to clean up the temporary file after restoration failure. File path: " + tempFile.getAbsolutePath());
            }
            // 恢复失败，返回 false
            return false;
        }
    }

    /**
     * 尝试删除指定路径的旧文件，最多重试 3 次
     * 每次删除失败后会等待 500 毫秒再进行下一次尝试
     *
     * @param path 要删除的旧文件的路径
     */
    private void deleteOldFileWithRetry(String path) {
        // 创建文件对象
        File oldFile = new File(path);
        int retryCount = 0;
        // 当文件存在且重试次数小于 3 时，继续尝试删除
        while (oldFile.exists() && retryCount < 3) {
            if (oldFile.delete()) {
                // 若删除成功，跳出循环
                break;
            }
            retryCount++;
            try {
                // 每次删除失败后，线程休眠 500 毫秒
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // 若线程被中断，恢复中断状态并记录错误日志
                Thread.currentThread().interrupt();
                Log.e(TAG, "Interrupted while deleting old file: " + path, e);
                break;
            }
        }
    }

    /**
     * 验证指定文件是否为有效的 SharedPreferences 文件
     * 通过检查文件的第一行是否以特定的 XML 声明开头来判断
     *
     * @param file 要验证的文件对象
     * @return 若文件有效返回 true，否则返回 false
     */
    private boolean isValidSharedPreferences(File file) {
        try (
                // 创建 BufferedReader 用于读取文件内容
                BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // 读取文件的第一行
            String firstLine = reader.readLine();
            // 检查第一行是否以特定的 XML 声明开头
            return firstLine != null && firstLine.startsWith("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>");
        } catch (IOException e) {
            // 若读取文件时出现异常，返回 false
            return false;
        }
    }

    /**
     * 根据传入的布尔值决定使用 commit 还是 apply 方法提交 SharedPreferences 的更改
     *
     * @param useCommit 若为 true 则使用 commit 方法，若为 false 则使用 apply 方法
     */
    private void commitOrApply(boolean useCommit) {
        if (useCommit) {
            // 使用 commit 方法提交更改，会同步操作并返回结果
            editor.commit();
        } else {
            // 使用 apply 方法提交更改，是异步操作
            editor.apply();
        }
    }

    /**
     * 检查 SharedPreferences 中是否包含指定键的数据
     * 会同时检查以 DATA_KEY_PREFIX 和 OBJECT_KEY_PREFIX 为前缀的键
     *
     * @param key 要检查的键
     * @return 若包含返回 true，否则返回 false
     */
    public boolean contains(@NonNull String key) {
        return preferences.contains(DATA_KEY_PREFIX + key) || preferences.contains(OBJECT_KEY_PREFIX + key);
    }

    /**
     * 从 SharedPreferences 中移除指定键的数据，使用默认的提交方式（apply）
     *
     * @param key 要移除的键
     */
    public void remove(@NonNull String key) {
        remove(key, false);
    }

    /**
     * 从 SharedPreferences 中移除指定键的数据，可以选择提交方式（commit 或 apply）
     * 会同时移除以 DATA_KEY_PREFIX 和 OBJECT_KEY_PREFIX 为前缀的键
     *
     * @param key       要移除的键
     * @param useCommit 若为 true 则使用 commit 方法，若为 false 则使用 apply 方法
     */
    public void remove(@NonNull String key, boolean useCommit) {
        // 移除以 DATA_KEY_PREFIX 为前缀的键
        editor.remove(DATA_KEY_PREFIX + key);
        // 移除以 OBJECT_KEY_PREFIX 为前缀的键
        editor.remove(OBJECT_KEY_PREFIX + key);
        // 根据 useCommit 的值选择提交方式
        commitOrApply(useCommit);
    }

    /**
     * 清空 SharedPreferences 中的所有数据，使用默认的提交方式（apply）
     * 同时会清空 Bitmap 缓存
     */
    public void clear() {
        clear(false);
    }

    /**
     * 清空 SharedPreferences 中的所有数据，可以选择提交方式（commit 或 apply）
     * 同时会清空 Bitmap 缓存
     *
     * @param useCommit 若为 true 则使用 commit 方法，若为 false 则使用 apply 方法
     */
    public void clear(boolean useCommit) {
        // 清空 SharedPreferences 中的所有数据
        editor.clear();
        // 根据 useCommit 的值选择提交方式
        commitOrApply(useCommit);
        // 清空 Bitmap 缓存
        bitmapCache.evictAll();
    }
}