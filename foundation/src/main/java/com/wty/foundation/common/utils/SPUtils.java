package com.wty.foundation.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;

public class SPUtils {
    private static final String PREF_NAME = "MyPrefsFile"; // SharedPreferences 文件名
    private static final String KEY_PREFIX = "key_"; // 存储键的前缀
    private static final String OBJECT_KEY_PREFIX = "obj_"; // 对象存储键的前缀
    private static final Gson GSON = new Gson(); // 使用 Gson 进行序列化和反序列化
    private static volatile SPUtils INSTANCE = null; // 单例实例
    private final SharedPreferences preferences; // SharedPreferences 实例

    /**
     * 私有构造函数，防止外部实例化。
     *
     * @param context 应用上下文
     */
    private SPUtils(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 初始化 SPUtils 单例。
     *
     * @param context 应用上下文
     * @return 初始化后的 SPUtils 单例
     */
    public static SPUtils initialize(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SPUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SPUtils(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 获取 SPUtils 单例。
     *
     * @return SPUtils 单例
     */
    public static SPUtils getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SPUtils 必须先初始化。");
        }
        return INSTANCE;
    }

    /**
     * 获取 SharedPreferences 编辑器。
     *
     * @return SharedPreferences.Editor
     */
    private SharedPreferences.Editor edit() {
        return preferences.edit();
    }

    /**
     * 存储字符串。
     *
     * @param key 键
     * @param value 值
     */
    public void putString(String key, String value) {
        edit().putString(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取字符串。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public String getString(String key, String defaultValue) {
        return preferences.getString(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储整型。
     *
     * @param key 键
     * @param value 值
     */
    public void putInt(String key, int value) {
        edit().putInt(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取整型。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 整型值
     */
    public int getInt(String key, int defaultValue) {
        return preferences.getInt(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储长整型。
     *
     * @param key 键
     * @param value 值
     */
    public void putLong(String key, long value) {
        edit().putLong(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取长整型。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 长整型值
     */
    public long getLong(String key, long defaultValue) {
        return preferences.getLong(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储浮点型。
     *
     * @param key 键
     * @param value 值
     */
    public void putFloat(String key, float value) {
        edit().putFloat(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取浮点型。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 浮点型值
     */
    public float getFloat(String key, float defaultValue) {
        return preferences.getFloat(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储双精度型。
     *
     * @param key 键
     * @param value 值
     */
    public void putDouble(String key, double value) {
        putLong(key, Double.doubleToRawLongBits(value));
    }

    /**
     * 获取双精度型。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 双精度型值
     */
    public double getDouble(String key, double defaultValue) {
        long bits = getLong(key, Double.doubleToRawLongBits(defaultValue));
        return Double.longBitsToDouble(bits);
    }

    /**
     * 存储布尔型。
     *
     * @param key 键
     * @param value 值
     */
    public void putBoolean(String key, boolean value) {
        edit().putBoolean(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取布尔型。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 布尔型值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(KEY_PREFIX + key, defaultValue);
    }

    /**
     * 存储字符串集合。
     *
     * @param key 键
     * @param value 值
     */
    public void putStringSet(String key, Set<String> value) {
        edit().putStringSet(KEY_PREFIX + key, value).apply();
    }

    /**
     * 获取字符串集合。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 字符串集合
     */
    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        return preferences.getStringSet(KEY_PREFIX + key, defaultValue != null ? defaultValue : new HashSet<>());
    }

    /**
     * 存储可序列化的对象。
     *
     * @param key 键
     * @param obj 对象
     */
    public <T extends Serializable> void putSerializableObject(String key, T obj) {
        String json = GSON.toJson(obj);
        edit().putString(OBJECT_KEY_PREFIX + key, json).apply();
    }

    /**
     * 获取可序列化的对象。
     *
     * @param key 键
     * @param clazz 类型
     * @param <T> 泛型类型
     * @return 反序列化的对象
     */
    public <T extends Serializable> T getSerializableObject(String key, Class<T> clazz) {
        String json = preferences.getString(OBJECT_KEY_PREFIX + key, null);
        if (json == null) {
            return null;
        }
        Type type = new TypeToken<T>() {}.getType();
        return GSON.fromJson(json, type);
    }

    /**
     * 存储 Parcelable 对象。
     *
     * @param key 键
     * @param obj 对象
     */
    public <T extends Parcelable> void putParcelableObject(String key, T obj) {
        Parcel parcel = Parcel.obtain();
        obj.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        putBytes(OBJECT_KEY_PREFIX + key, bytes);
    }

    /**
     * 获取 Parcelable 对象。
     *
     * @param key 键
     * @param clazz 类型
     * @param <T> 泛型类型
     * @return 反序列化的对象
     */
    public <T extends Parcelable> T getParcelableObject(String key, Class<T> clazz) {
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
     * 存储字节数组。
     *
     * @param key 键
     * @param value 字节数组
     */
    public void putBytes(String key, byte[] value) {
        edit().putString(key, encodeBytesToBase64(value)).apply();
    }

    /**
     * 获取字节数组。
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 字节数组
     */
    public byte[] getBytes(String key, byte[] defaultValue) {
        String encoded = getString(key, null);
        if (encoded == null) {
            return defaultValue;
        }
        return decodeBase64ToBytes(encoded);
    }

    /**
     * 存储 Parcelable 列表。
     *
     * @param key 键
     * @param list 列表
     * @param <T> 泛型类型
     */
    public <T extends Parcelable> void putParcelableList(String key, List<T> list) {
        Parcel parcel = Parcel.obtain();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (T item : list) {
                parcel.writeParcelable(item, 0);
                byte[] marshalledBytes = parcel.marshall();
                baos.write(marshalledBytes); // 直接写入字节数组
                parcel.recycle();
                parcel = Parcel.obtain(); // 重置 Parcel 的状态
            }
            byte[] bytes = baos.toByteArray();
            putBytes(OBJECT_KEY_PREFIX + key, bytes);
        } catch (IOException e) {
            throw new RuntimeException("将 Parcelable 列表写入字节时出错", e);
        } finally {
            parcel.recycle(); // 确保 Parcel 被回收
        }
    }

    /**
     * 获取 Parcelable 列表。
     *
     * @param key 键
     * @param clazz 类型
     * @param <T> 泛型类型
     * @return 反序列化的列表
     */
    public <T extends Parcelable> List<T> getParcelableList(String key, Class<T> clazz) {
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
            // 处理可能出现的 BadParcelableException 异常
            throw new RuntimeException("从字节读取 Parcelable 列表时出错", e);
        } finally {
            parcel.recycle();
        }
        return list;
    }

    /**
     * 检查是否包含某个键。
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean contains(String key) {
        return preferences.contains(KEY_PREFIX + key);
    }

    /**
     * 删除某个键对应的值。
     *
     * @param key 键
     */
    public void remove(String key) {
        edit().remove(KEY_PREFIX + key).apply();
    }

    /**
     * 清空 SharedPreferences 中的所有数据。
     */
    public void clear() {
        edit().clear().apply();
    }

    /**
     * 将字节数组编码为 Base64 字符串。
     *
     * @param value 字节数组
     * @return 编码后的字符串
     */
    private static String encodeBytesToBase64(byte[] value) {
        return Base64.encodeToString(value, Base64.DEFAULT);
    }

    /**
     * 将 Base64 字符串解码为字节数组。
     *
     * @param encoded 编码后的字符串
     * @return 解码后的字节数组
     */
    private static byte[] decodeBase64ToBytes(String encoded) {
        return Base64.decode(encoded, Base64.DEFAULT);
    }
}