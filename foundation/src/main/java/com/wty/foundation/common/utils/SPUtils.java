package com.wty.foundation.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wty.foundation.common.init.AppContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

public class SPUtils {
    private static final String TAG = "SPUtils";
    private static final String PREF_NAME = "MyPrefsFile";
    private static final String KEY_PREFIX = "key_";
    private static final String OBJECT_KEY_PREFIX = "obj_";
    private static final Gson GSON = new Gson();
    private static volatile SPUtils INSTANCE = null;
    private final SharedPreferences preferences;
    private SharedPreferences.Editor editor;

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
     * @param key 键
     * @param value 值
     */
    public void putString(@NonNull String key, String value) {
        putString(key, value, false);
    }

    /**
     * 存储一个字符串值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的字符串值或默认值
     */
    public String getString(@NonNull String key, String defaultValue) {
        return preferences.getString(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个整数值
     *
     * @param key 键
     * @param value 值
     */
    public void putInt(@NonNull String key, int value) {
        putInt(key, value, false);
    }

    /**
     * 存储一个整数值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的整数值或默认值
     */
    public int getInt(@NonNull String key, int defaultValue) {
        return preferences.getInt(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个长整数值
     *
     * @param key 键
     * @param value 值
     */
    public void putLong(@NonNull String key, long value) {
        putLong(key, value, false);
    }

    /**
     * 存储一个长整数值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的长整数值或默认值
     */
    public long getLong(@NonNull String key, long defaultValue) {
        return preferences.getLong(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个浮点数值
     *
     * @param key 键
     * @param value 值
     */
    public void putFloat(@NonNull String key, float value) {
        putFloat(key, value, false);
    }

    /**
     * 存储一个浮点数值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的浮点数值或默认值
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        return preferences.getFloat(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个双精度浮点数值
     *
     * @param key 键
     * @param value 值
     */
    public void putDouble(@NonNull String key, double value) {
        putDouble(key, value, false);
    }

    /**
     * 存储一个双精度浮点数值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
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
     * @param key 键
     * @param value 值
     */
    public void putBoolean(@NonNull String key, boolean value) {
        putBoolean(key, value, false);
    }

    /**
     * 存储一个布尔值
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的布尔值或默认值
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return preferences.getBoolean(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储一个字符串集合
     *
     * @param key 键
     * @param value 值
     */
    public void putStringSet(@NonNull String key, Set<String> value) {
        putStringSet(key, value, false);
    }

    /**
     * 存储一个字符串集合
     *
     * @param key 键
     * @param value 值
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
     * @param key 键
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
     * @param key 键
     * @param obj 对象
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
     * @param key 键
     * @param clazz 对象的类
     * @param <T> 泛型类型
     * @return 存储的可序列化对象
     */
    public <T extends Serializable> T getSerializableObject(@NonNull String key, Class<T> clazz) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            return null;
        }
        return GSON.fromJson(json, clazz);
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
     * 存储一个 Parcelable 对象
     *
     * @param key 键
     * @param obj 对象
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
     * 获取一个 Parcelable 对象
     *
     * @param key 键
     * @param clazz 对象的类
     * @param <T> 泛型类型
     * @return 存储的 Parcelable 对象
     */
    public <T extends Parcelable> T getParcelableObject(@NonNull String key, Class<T> clazz) {
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) {
            return null;
        }
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T parcelable = clazz.cast(parcel.readParcelable(clazz.getClassLoader()));
        parcel.recycle();
        return parcelable;
    }

    /**
     * 存储一个字节数组
     *
     * @param key 键
     * @param value 字节数组
     */
    public void putBytes(@NonNull String key, byte[] value) {
        putBytes(key, value, false);
    }

    /**
     * 存储一个字节数组
     *
     * @param key 键
     * @param value 字节数组
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void putBytes(@NonNull String key, byte[] value, boolean useCommit) {
        editor.putString(key, encodeBytesToBase64(value));
        commitOrApply(useCommit);
    }

    /**
     * 获取一个字节数组
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 存储的字节数组或默认值
     */
    public byte[] getBytes(@NonNull String key, byte[] defaultValue) {
        String encoded = getString(key, null);
        if (encoded == null) {
            return defaultValue;
        }
        return decodeBase64ToBytes(encoded);
    }

    /**
     * 存储一个 Parcelable 列表
     *
     * @param key 键
     * @param list 列表
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list) {
        putParcelableList(key, list, false);
    }

    /**
     * 存储一个 Parcelable 列表
     *
     * @param key 键
     * @param list 列表
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public <T extends Parcelable> void putParcelableList(@NonNull String key, List<T> list, boolean useCommit) {
        Parcel parcel = Parcel.obtain();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (T item : list) {
                parcel.writeParcelable(item, 0);
                byte[] marshalledBytes = parcel.marshall();
                baos.write(marshalledBytes);
                parcel.recycle();
                parcel = Parcel.obtain();
            }
            byte[] bytes = baos.toByteArray();
            putBytes(OBJECT_KEY_PREFIX + key, bytes, useCommit);
        } catch (IOException e) {
            throw new RuntimeException("将 Parcelable 列表写入字节时出错", e);
        } finally {
            parcel.recycle();
        }
    }

    /**
     * 获取一个 Parcelable 列表
     *
     * @param key 键
     * @param clazz 列表中对象的类
     * @param <T> 泛型类型
     * @return 存储的 Parcelable 列表
     */
    public <T extends Parcelable> List<T> getParcelableList(@NonNull String key, Class<T> clazz) {
        byte[] bytes = getBytes(OBJECT_KEY_PREFIX + key, null);
        if (bytes == null) {
            return new ArrayList<>();
        }
        Parcel parcel = Parcel.obtain();
        List<T> list = new ArrayList<>();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            while (parcel.dataPosition() < bytes.length) {
                T item = clazz.cast(parcel.readParcelable(clazz.getClassLoader()));
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("从字节读取 Parcelable 列表时出错", e);
        } finally {
            parcel.recycle();
        }
        return list;
    }

    /**
     * 获取所有存储的键值对
     *
     * @return 包含所有键值对的 Map
     */
    public Map<String, Object> getAllEntries() {
        Map<String, ?> allEntries = preferences.getAll();
        Map<String, Object> convertedEntries = new LinkedHashMap<>();
        if (allEntries == null) {
            return convertedEntries;
        }
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
                Type type = new TypeToken<Serializable>() {}.getType();
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
     * 移除一个键值对
     *
     * @param key 要移除的键
     * @param useCommit 如果为 true，则使用 .commit() 提交更改；否则使用 .apply()
     */
    public void remove(@NonNull String key, boolean useCommit) {
        editor.remove(KEY_PREFIX + key);
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
     *
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
