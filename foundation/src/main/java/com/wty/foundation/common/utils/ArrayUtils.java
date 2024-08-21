package com.wty.foundation.common.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * 数据、集合等相关工具类
 */
public class ArrayUtils {
    private ArrayUtils() {}

    /**
     * List 是null或者empty
     *
     * @param list List
     * @return boolean
     */
    public static boolean isEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * List 是null或者empty
     *
     * @param set List
     * @return boolean
     */
    public static boolean isEmpty(@Nullable Set<?> set) {
        return set == null || set.isEmpty();
    }

    /**
     * Map 是null或者empty
     *
     * @param map map
     * @return boolean
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Object[] 是null或者empty
     *
     * @param objects Object[]
     * @return boolean
     */
    public static boolean isEmpty(@Nullable Object[] objects) {
        return objects == null || objects.length == 0;
    }

    /**
     * int[] 是null或者empty
     *
     * @param ints int[]
     * @return boolean
     */
    public static boolean isEmpty(@Nullable int[] ints) {
        return ints == null || ints.length == 0;
    }

    /**
     * List的长度
     *
     * @param list list
     * @return int
     */
    public static int size(@Nullable List<?> list) {
        return isEmpty(list) ? 0 : list.size();
    }

    /**
     * List的长度
     *
     * @param set list
     * @return int
     */
    public static int size(@Nullable Set<?> set) {
        return isEmpty(set) ? 0 : set.size();
    }

    /**
     * Map的长度
     *
     * @param map Map
     * @return int
     */
    public static int size(@Nullable Map<?, ?> map) {
        return isEmpty(map) ? 0 : map.size();
    }

    /**
     * Object[]的长度
     *
     * @param objects Object[]
     * @return int
     */
    public static int size(@Nullable Object[] objects) {
        return isEmpty(objects) ? 0 : objects.length;
    }

    /**
     * int[]的长度
     *
     * @param ints int[]
     * @return int
     */
    public static int size(@Nullable int[] ints) {
        return isEmpty(ints) ? 0 : ints.length;
    }

    /**
     * 获取指定位置的元素
     *
     * @param list 集合
     * @param index 下标
     * @param <T> 元素类型
     * @return 元素，可能为空
     */
    @Nullable
    public static <T> T get(@Nullable List<T> list, int index) {
        return index < 0 || index >= size(list) ? null : isEmpty(list) ? null : list.get(index);
    }

    /**
     * 获取指定位置的元素
     *
     * @param map 集合
     * @param key 下标
     * @param <T> 元素类型
     * @return 元素，可能为空
     */
    @Nullable
    public static <E, T> T get(@Nullable Map<E, T> map, E key) {
        return isEmpty(map) ? null : map.get(key);
    }

    /**
     * 获取指定位置的元素
     *
     * @param objects 数组
     * @param index 下标
     * @param <T> 元素类型
     * @return 元素，可能为空
     */
    @Nullable
    public static <T> T get(@Nullable Object[] objects, int index) {
        return index < 0 || index >= size(objects) ? null : isEmpty(objects) ? null : (T)objects[index];
    }

    /**
     * 获取指定位置的元素
     *
     * @param ints 数组
     * @param index 下标
     * @return 元素，没有查找到返回0
     */
    public static int get(@Nullable int[] ints, int index) {
        return get(ints, 0);
    }

    /**
     * 获取指定位置的元素
     *
     * @param ints 数组
     * @param index 下标
     * @param defaultValue 默认值
     * @return 元素，没有查找到返回默认值
     */
    public static int get(@Nullable int[] ints, int index, int defaultValue) {
        return index < 0 || index >= size(ints) ? defaultValue : isEmpty(ints) ? defaultValue : ints[index];
    }
}
