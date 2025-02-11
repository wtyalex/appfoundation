package com.wty.foundation.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wty.foundation.common.init.AppContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class SPUtils {
    private static final String TAG = "SPUtils";
    private static final String PREF_NAME = "MyPrefsFile";
    private static final String KEY_PREFIX = "key_";
    private static final String OBJECT_KEY_PREFIX = "obj_";
    private static final Gson GSON = new Gson();
    private static volatile SPUtils INSTANCE = null;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private final ConcurrentLinkedQueue<Runnable> transactionOperations = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> rollbackOperations = new ConcurrentLinkedQueue<>();
    private final Object rollbackLock = new Object();
    private final Map<String, Object> originalValues = new HashMap<>();
    private final ReentrantLock bitmapOperationLock = new ReentrantLock();
    private final Map<String, Bitmap> bitmapCache = new HashMap<>();
    private final List<WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>> listeners = new ArrayList<>();
    private final Object listenerLock = new Object();

    private SPUtils() {
        Context context = AppContext.getInstance().getContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = preferences.edit();
    }

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
     * 存储一个字符串值
     *
     * @param key   键
     * @param value 值
     */
    public void putString(@NonNull String key, String value) {
        putString(key, value, false);
    }

    /**
     * 存储一个字符串值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putString(@NonNull String key, String value, boolean useCommit) {
        editor.putString(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字符串值
     *
     * @param key 键
     * @return 默认值 ""
     */
    public String getString(@NonNull String key) {
        return preferences.getString(KEY_PREFIX + key, "");
    }

    /**
     * 获取一个字符串值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的字符串值或默认值
     */
    public String getString(@NonNull String key, String defaultValue) {
        return preferences.getString(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个整数值
     *
     * @param key   键
     * @param value 值
     */
    public void putInt(@NonNull String key, int value) {
        putInt(key, value, false);
    }

    /**
     * 存储一个整数值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putInt(@NonNull String key, int value, boolean useCommit) {
        editor.putInt(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个整数值
     *
     * @param key 键
     * @return 默认值 -1
     */
    public int getInt(@NonNull String key) {
        return preferences.getInt(KEY_PREFIX + key, -1);
    }

    /**
     * 获取一个整数值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的整数值或默认值
     */
    public int getInt(@NonNull String key, int defaultValue) {
        return preferences.getInt(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个长整数值
     *
     * @param key   键
     * @param value 值
     */
    public void putLong(@NonNull String key, long value) {
        putLong(key, value, false);
    }

    /**
     * 存储一个长整数值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putLong(@NonNull String key, long value, boolean useCommit) {
        editor.putLong(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个长整数值
     *
     * @param key 键
     * @return 默认值 -1L
     */
    public long getLong(@NonNull String key) {
        return preferences.getLong(KEY_PREFIX + key, -1L);
    }

    /**
     * 获取一个长整数值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的长整数值或默认值
     */
    public long getLong(@NonNull String key, long defaultValue) {
        return preferences.getLong(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个浮点数值
     *
     * @param key   键
     * @param value 值
     */
    public void putFloat(@NonNull String key, float value) {
        putFloat(key, value, false);
    }

    /**
     * 存储一个浮点数值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putFloat(@NonNull String key, float value, boolean useCommit) {
        editor.putFloat(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个浮点数值
     *
     * @param key 键
     * @return 默认值 -1f
     */
    public float getFloat(@NonNull String key) {
        return preferences.getFloat(KEY_PREFIX + key, -1f);
    }

    /**
     * 获取一个浮点数值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的浮点数值或默认值
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        return preferences.getFloat(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个双精度浮点数值
     *
     * @param key   键
     * @param value 值
     */
    public void putDouble(@NonNull String key, double value) {
        putDouble(key, value, false);
    }

    /**
     * 存储一个双精度浮点数值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putDouble(@NonNull String key, double value, boolean useCommit) {
        putLong(key, Double.doubleToRawLongBits(value), useCommit);
    }

    /**
     * 获取一个双精度浮点数值
     *
     * @param key 键
     * @return 默认值 -1L
     */
    public double getDouble(@NonNull String key) {
        long bits = getLong(key, Double.doubleToRawLongBits(-1L));
        return Double.longBitsToDouble(bits);
    }

    /**
     * 获取一个双精度浮点数值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的双精度浮点数值或默认值
     */
    public double getDouble(@NonNull String key, double defaultValue) {
        long bits = getLong(key, Double.doubleToRawLongBits(defaultValue));
        return Double.longBitsToDouble(bits);
    }

    /**
     * 存储一个布尔值
     *
     * @param key   键
     * @param value 值
     */
    public void putBoolean(@NonNull String key, boolean value) {
        putBoolean(key, value, false);
    }

    /**
     * 存储一个布尔值
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putBoolean(@NonNull String key, boolean value, boolean useCommit) {
        editor.putBoolean(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个布尔值
     *
     * @param key 键
     * @return 默认值 false
     */
    public boolean getBoolean(@NonNull String key) {
        return preferences.getBoolean(KEY_PREFIX + key, false);
    }

    /**
     * 获取一个布尔值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的布尔值或默认值
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return preferences.getBoolean(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个字符串集合
     *
     * @param key   键
     * @param value 值
     */
    public void putStringSet(@NonNull String key, Set<String> value) {
        putStringSet(key, value, false);
    }

    /**
     * 存储一个字符串集合
     *
     * @param key       键
     * @param value     值
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putStringSet(@NonNull String key, Set<String> value, boolean useCommit) {
        editor.putStringSet(KEY_PREFIX + key, value);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字符串集合
     *
     * @param key 键
     * @return 默认值
     */
    public Set<String> getStringSet(@NonNull String key) {
        return preferences.getStringSet(KEY_PREFIX + key, Collections.<String>emptySet());
    }

    /**
     * 获取一个字符串集合
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的字符串集合或默认值
     */
    public Set<String> getStringSet(@NonNull String key, Set<String> defaultValue) {
        return preferences.getStringSet(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个可序列化的对象
     *
     * @param key 键
     * @param obj 对象
     */
    public <T extends Serializable> void putSerializableObject(@NonNull String key, T obj) {
        putSerializableObject(key, obj, false);
    }

    /**
     * 存储一个可序列化的对象
     *
     * @param key       键
     * @param obj       对象
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <T extends Serializable> void putSerializableObject(@NonNull String key, T obj, boolean useCommit) {
        String json = GSON.toJson(obj);
        editor.putString(OBJECT_KEY_PREFIX + key, json);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个可序列化的对象
     *
     * @param key           键
     * @param clazz         对象的类
     * @param <T>           泛型类型
     * @param defaultObject 如果找不到指定键的数据则返回此默认对象
     * @return 存储的可序列化对象或提供的默认对象
     */
    public <T extends Serializable> T getSerializableObject(@NonNull String key, Class<T> clazz, T defaultObject) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            Log.w(TAG, "No serializable object found for key: " + key + ". Returning default object.");
            return defaultObject;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            Log.e(TAG, "Failed to deserialize object from JSON for key: " + key, e);
            return defaultObject;
        }
    }

    /**
     * 存储一个 Parcelable 对象
     *
     * @param key 键
     * @param obj 对象
     */
    public <T extends Parcelable> void putParcelableObject(@NonNull String key, T obj) {
        putParcelableObject(key, obj, false);
    }

    /**
     * 存储 Parcelable 对象
     *
     * @param key       键
     * @param obj       要存储的 Parcelable 对象
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <T extends Parcelable> void putParcelableObject(@NonNull String key, T obj, boolean useCommit) {
        Parcel parcel = Parcel.obtain();
        obj.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        putBytes(OBJECT_KEY_PREFIX + key, bytes, useCommit);
    }

    /**
     * 获取 Parcelable 对象
     *
     * @param key           键
     * @param clazz         对象的类
     * @param defaultObject 如果找不到或反序列化失败时返回的默认对象
     * @param <T>           泛型类型
     * @return 存储的对象或默认值
     */
    public <T extends Parcelable> T getParcelableObject(@NonNull String key, Class<T> clazz, T defaultObject) {
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) {
            Log.w(TAG, "未找到 Parcelable 对象对应的键: " + key + " 返回默认对象 ");
            return defaultObject;
        }
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            return clazz.getConstructor(Parcel.class).newInstance(parcel);
        } catch (Exception e) {
            Log.e(TAG, "从字节反序列化 Parcelable 对象时出错: " + key, e);
            return defaultObject;
        } finally {
            parcel.recycle();
        }
    }

    /**
     * 存储一个 Serializable 列表
     *
     * @param key  键
     * @param list 列表
     */
    public <T extends Serializable> void putSerializableList(@NonNull String key, List<T> list) {
        putSerializableList(key, list, false);
    }

    /**
     * 存储一个 Serializable 列表
     *
     * @param key       键
     * @param list      列表
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <T extends Serializable> void putSerializableList(@NonNull String key, List<T> list, boolean useCommit) {
        String json = GSON.toJson(list);
        editor.putString(OBJECT_KEY_PREFIX + key, json);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个 Serializable 列表
     *
     * @param key   键
     * @param clazz 列表中对象的类
     * @param <T>   泛型类型
     * @return 存储的 Serializable 列表
     */
    public <T extends Serializable> List<T> getSerializableList(@NonNull String key, Class<T> clazz) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            return new ArrayList<>();
        }
        return GSON.fromJson(json, TypeToken.getParameterized(List.class, clazz).getType());
    }

    /**
     * 存储一个 Parcelable 列表
     *
     * @param key  键
     * @param list 列表
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list) {
        putParcelableList(key, list, false);
    }

    /**
     * 存储一个 Parcelable 列表
     *
     * @param key       键
     * @param list      要存储的 Parcelable 对象列表
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list, boolean useCommit) {
        Parcel parcel = Parcel.obtain();
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) { // 使用 try-with-resources 确保流被关闭
            for (T item : list) {
                parcel.writeParcelable(item, 0);
                byte[] marshalledBytes = parcel.marshall();
                stream.write(marshalledBytes);
                parcel.setDataPosition(0); // 重置数据位置以便写入下一个对象
            }
            byte[] bytes = stream.toByteArray();
            putBytes(OBJECT_KEY_PREFIX + key, bytes, useCommit);
        } catch (IOException e) {
            throw new RuntimeException("将 Parcelable 列表写入字节时出错", e);
        } finally {
            parcel.recycle(); // 回收parcel资源
        }
    }

    /**
     * 获取一个 Parcelable 列表
     *
     * @param key   键
     * @param clazz 列表中对象的类
     * @param <T>   泛型类型
     * @return 存储的 Parcelable 列表或空列表
     */
    public <T extends Parcelable> List<T> getParcelableList(@NonNull String key, Class<T> clazz) {
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) {
            return Collections.emptyList(); // 返回不可变空列表作为默认值
        }
        Parcel parcel = Parcel.obtain();
        List<T> list = new ArrayList<>();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            while (parcel.dataPosition() < bytes.length) {
                T item = parcel.readParcelable(clazz.getClassLoader());
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "从字节读取 Parcelable 列表时出错: " + key, e);
        } finally {
            parcel.recycle(); // 回收parcel资源
        }
        return list;
    }

    /**
     * 存储一个 Map
     *
     * @param key 键
     * @param map 映射
     */
    public <K, V> void putMap(@NonNull String key, Map<K, V> map) {
        putMap(key, map, false);
    }

    /**
     * 存储一个 Map
     *
     * @param key       键
     * @param map       映射
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <K, V> void putMap(@NonNull String key, Map<K, V> map, boolean useCommit) {
        String json = GSON.toJson(map);
        editor.putString(OBJECT_KEY_PREFIX + key, json);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个 Map
     *
     * @param key        键
     * @param keyClass   映射键的类
     * @param valueClass 映射值的类
     * @param <K>        键的泛型类型
     * @param <V>        值的泛型类型
     * @return 存储的 Map
     */
    public <K, V> Map<K, V> getMap(@NonNull String key, Class<K> keyClass, Class<V> valueClass) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            return new HashMap<>();
        }
        return GSON.fromJson(json, TypeToken.getParameterized(Map.class, keyClass, valueClass).getType());
    }

    /**
     * 存储一个 Map 列表
     *
     * @param key  键
     * @param list 列表
     */
    public <K, V> void putMapList(@NonNull String key, List<Map<K, V>> list) {
        putMapList(key, list, false);
    }

    /**
     * 存储一个 Map 列表
     *
     * @param key       键
     * @param list      列表
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <K, V> void putMapList(@NonNull String key, List<Map<K, V>> list, boolean useCommit) {
        String json = GSON.toJson(list);
        editor.putString(OBJECT_KEY_PREFIX + key, json);
        commitOrApply(useCommit);
    }

    /**
     * 获取一个 Map 列表
     *
     * @param key        键
     * @param keyClass   映射键的类
     * @param valueClass 映射值的类
     * @param <K>        键的泛型类型
     * @param <V>        值的泛型类型
     * @return 存储的 Map 列表
     */
    public <K, V> List<Map<K, V>> getMapList(@NonNull String key, Class<K> keyClass, Class<V> valueClass) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            return new ArrayList<>();
        }
        return GSON.fromJson(json, TypeToken.getParameterized(List.class, Map.class, keyClass, valueClass).getType());
    }

    /**
     * 存储一个 Bitmap 图像
     *
     * @param key    键
     * @param bitmap 要存储的位图
     */
    public void putBitmap(@NonNull String key, Bitmap bitmap) {
        putBitmap(KEY_PREFIX + key, bitmap, 50, false);
    }

    /**
     * 存储一个 Bitmap 图像
     *
     * @param key       键
     * @param bitmap    要存储的位图
     * @param quality   压缩质量（0-100）
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putBitmap(@NonNull String key, Bitmap bitmap, int quality, boolean useCommit) {
        bitmapOperationLock.lock();
        try {
            Context context = AppContext.getInstance().getContext();
            String fileName = UUID.randomUUID().toString() + ".png"; // 使用 UUID 生成唯一文件名
            File file = new File(context.getFilesDir(), fileName);

            // 先存储新文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos)) {
                    throw new IOException("Failed to compress and save bitmap to file.");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while saving bitmap to file: " + key, e);
                if (file.exists() && !file.delete()) {
                    Log.w(TAG, "Failed to delete invalid bitmap file: " + file.getAbsolutePath());
                }
                throw new RuntimeException(e);
            }

            // 删除旧文件
            String oldFilePath = preferences.getString(OBJECT_KEY_PREFIX + key, null);
            if (oldFilePath != null) {
                File oldFile = new File(oldFilePath);
                if (oldFile.exists() && !oldFile.delete()) {
                    Log.w(TAG, "Failed to delete old bitmap file: " + oldFilePath);
                }
            }

            // 保存新文件路径到 SharedPreferences
            editor.putString(OBJECT_KEY_PREFIX + key, file.getAbsolutePath());

            // 提交更改
            commitOrApply(useCommit);

            // 更新缓存
            bitmapCache.put(key, bitmap);
        } finally {
            bitmapOperationLock.unlock();
        }
    }

    /**
     * 根据键获取存储的 Bitmap
     *
     * @param key           键
     * @param defaultBitmap 如果找不到或解码失败时返回的默认 Bitmap
     * @return 存储的 Bitmap 或默认值
     */
    public Bitmap getBitmap(@NonNull String key, Bitmap defaultBitmap) {
        if (bitmapCache.containsKey(key)) {
            return bitmapCache.get(key);
        }
        String filePath = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (filePath != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    bitmapCache.put(key, bitmap); // 缓存加载的 Bitmap
                    return bitmap;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while decoding bitmap from file: " + filePath, e);
            }
        } else {
            Log.w(TAG, "No bitmap file found for key: " + key);
        }
        return defaultBitmap;
    }

    /**
     * 移除指定键的 Bitmap 文件
     *
     * @param key       键
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void removeBitmap(@NonNull String key, boolean useCommit) {
        String filePath = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Failed to delete bitmap file: " + filePath);
            }
            editor.remove(OBJECT_KEY_PREFIX + key);
            commitOrApply(useCommit);
        }
    }

    /**
     * 存储一个字节数组
     *
     * @param key   键
     * @param value 字节数组
     */
    public void putBytes(@NonNull String key, byte[] value) {
        putBytes(key, value, false);
    }

    /**
     * 存储一个字节数组
     *
     * @param key       键
     * @param value     字节数组
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putBytes(@NonNull String key, byte[] value, boolean useCommit) {
        editor.putString(key, encodeBytesToBase64(value));
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字节数组
     *
     * @param key          键
     * @param defaultValue 默认值，如果找不到指定键的数据则返回此默认值
     * @return 存储的字节数组或默认值
     */
    public byte[] getBytes(@NonNull String key, byte[] defaultValue) {
        String encoded = getString(OBJECT_KEY_PREFIX + key, null);
        if (encoded == null) {
            return defaultValue;
        }
        return decodeBase64ToBytes(encoded);
    }

    /**
     * 获取所有存储的键值对
     *
     * @return 包含所有键值对的 Map
     */
    public Map<String, Object> getAllEntries() {
        Map<String, ?> allEntries = preferences.getAll();
        if (allEntries == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> convertedEntries = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                Log.w(TAG, "Skipping null key or value in getAllEntries.");
                continue;
            }
            if (!key.startsWith(OBJECT_KEY_PREFIX)) {
                convertedEntries.put(key, value);
                continue;
            }
            String jsonValue = value.toString();
            if (jsonValue == null) {
                Log.w(TAG, "Skipping null JSON value in getAllEntries.");
                continue;
            }
            try {
                Type type = new TypeToken<Serializable>() {
                }.getType();
                Object deserializedObject = GSON.fromJson(jsonValue, type);
                convertedEntries.put(key, deserializedObject);
            } catch (Exception e) {
                convertedEntries.put(key, jsonValue);
                Log.e(TAG, "Failed to deserialize object from JSON: " + key, e);
            }
        }
        return convertedEntries;
    }

    /**
     * 开始一个新的事务，清除所有之前的操作和回滚状态
     */
    public void beginTransaction() {
        transactionOperations.clear();
        rollbackOperations.clear();
        originalValues.clear(); // 清除原有的回滚状态
    }

    /**
     * 将一个操作及其对应的回滚操作添加到当前事务中，并自动记录回滚所需的状态
     *
     * @param operation 要执行的操作
     * @param key       操作相关的键，用于回滚时定位
     * @param rollback  如果操作失败时需要执行的回滚操作（可选）
     */
    public void addToTransaction(Runnable operation, String key, Runnable rollback) {
        if (key != null && preferences.contains(key)) {
            originalValues.put(key, preferences.getAll().get(key)); // 记录原始值
        }
        transactionOperations.add(operation);
        if (rollback != null) {
            rollbackOperations.add(rollback); // 添加手动回滚操作
        }
    }

    /**
     * 提交当前事务中的所有操作
     * 如果任何一个操作失败，则会尝试回滚所有已完成的操作以保证数据的一致性
     *
     * @throws Exception 如果事务提交过程中出现任何错误
     */
    public void commitTransaction() throws Exception {
        boolean success = true;
        Iterator<Runnable> operationIterator = transactionOperations.iterator();

        int index = 0;
        while (operationIterator.hasNext()) {
            try {
                operationIterator.next().run();
                operationIterator.remove(); // 移除已执行的操作
            } catch (Exception e) {
                success = false;
                Log.e(TAG, "事务在操作 " + index + " 处失败", e);
                break;
            }
            index++;
        }

        if (!success) {
            try {
                rollbackTransaction();
            } catch (Exception rollbackException) {
                Log.e(TAG, "回滚事务失败", rollbackException);
                throw new Exception("事务提交失败且回滚也失败", rollbackException);
            }
        } else {
            editor.apply();
        }
        transactionOperations.clear();
        rollbackOperations.clear();
    }

    /**
     * 回滚所有已执行的操作
     * 首先尝试执行手动回滚操作，然后根据记录的原始状态恢复数据
     */
    private void rollbackTransaction() {
        synchronized (rollbackLock) {
            Iterator<Runnable> rollbackIterator = rollbackOperations.iterator();
            while (rollbackIterator.hasNext()) {
                try {
                    rollbackIterator.next().run();
                    rollbackIterator.remove();
                } catch (Exception e) {
                    Log.e(TAG, "手动回滚操作失败", e);
                }
            }

            for (Map.Entry<String, Object> entry : originalValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String && key.startsWith(KEY_PREFIX)) {
                    // 处理 Bitmap 文件路径的回滚
                    String filePath = (String) value;
                    if (filePath != null && !filePath.isEmpty()) {
                        File file = new File(filePath);
                        if (!file.exists()) {
                            editor.remove(key); // 如果原始文件不存在，则移除该键
                        }
                    }
                }

                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Set<?>) {
                    @SuppressWarnings("unchecked") Set<String> set = (Set<String>) value;
                    editor.putStringSet(key, set);
                } else if (value instanceof Serializable) {
                    editor.putString(key, GSON.toJson(value));
                }
            }
            editor.apply();
        }
    }

    /**
     * 注册一个共享偏好设置变化监听器
     *
     * @param listener 监听器实例
     */
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (listenerLock) {
            listeners.add(new WeakReference<>(listener));
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * 移除一个共享偏好设置变化监听器
     *
     * @param listener 要移除的监听器实例
     */
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (listenerLock) {
            Iterator<WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> ref = iterator.next();
                SharedPreferences.OnSharedPreferenceChangeListener l = ref.get();
                if (l == null || l.equals(listener)) {
                    iterator.remove();
                    if (l != null) {
                        preferences.unregisterOnSharedPreferenceChangeListener(l);
                    }
                }
            }
        }
    }

    /**
     * 备份 SharedPreferences 到指定文件
     *
     * @param backupFile 备份文件
     */
    public void backupSharedPreferences(File backupFile) {
        Context context = AppContext.getInstance().getContext();
        File prefFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");
        try (InputStream inputStream = new FileInputStream(prefFile); OutputStream outputStream = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "SharedPreferences 文件未找到", e);
        } catch (IOException e) {
            Log.e(TAG, "备份 SharedPreferences 失败", e);
        }
    }

    /**
     * 从指定备份文件恢复 SharedPreferences
     *
     * @param backupFile 备份文件
     */
    public void restoreSharedPreferences(File backupFile) {
        Context context = AppContext.getInstance().getContext();
        File prefFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");
        try (InputStream inputStream = new FileInputStream(backupFile); OutputStream outputStream = new FileOutputStream(prefFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            this.editor = preferences.edit();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "备份文件未找到", e);
        } catch (IOException e) {
            Log.e(TAG, "恢复 SharedPreferences 失败", e);
        }
    }

    /**
     * 检查是否包含某个键
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean contains(@NonNull String key) {
        return preferences.contains(OBJECT_KEY_PREFIX + key);
    }

    /**
     * 移除一个键值对
     *
     * @param key 要移除的键
     */
    public void remove(@NonNull String key) {
        remove(key, false);
    }

    /**
     * 移除指定键的值
     *
     * @param key       键
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void remove(@NonNull String key, boolean useCommit) {
        if (preferences.contains(KEY_PREFIX + key)) {
            editor.remove(KEY_PREFIX + key);
        }
        if (preferences.contains(OBJECT_KEY_PREFIX + key)) {
            editor.remove(OBJECT_KEY_PREFIX + key);
        }
        commitOrApply(useCommit);
    }

    /**
     * 清除所有存储的数据
     */
    public void clear() {
        clear(false);
    }

    /**
     * 清除所有存储的数据
     *
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void clear(boolean useCommit) {
        editor.clear();
        commitOrApply(useCommit);
    }

    /**
     * 提交更改到 SharedPreferences
     * <p>
     * 默认使用 .apply() 方法，因为它异步执行且不会阻塞主线程 如果需要同步提交，请设置 useCommit 为 true
     *
     * @param useCommit 如果为 true，则使用 .commit() 方法（同步）；否则使用 .apply() 方法（异步）
     */
    private void commitOrApply(boolean useCommit) {
        if (useCommit) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * 将字节数组编码为 Base64 字符串
     *
     * @param value 字节数组
     * @return 编码后的字符串
     */
    private String encodeBytesToBase64(byte[] value) {
        return Base64.encodeToString(value, Base64.DEFAULT);
    }

    /**
     * 将 Base64 字符串解码为字节数组
     *
     * @param encoded 编码后的字符串
     * @return 解码后的字节数组
     */
    private byte[] decodeBase64ToBytes(String encoded) {
        return Base64.decode(encoded, Base64.DEFAULT);
    }
}
