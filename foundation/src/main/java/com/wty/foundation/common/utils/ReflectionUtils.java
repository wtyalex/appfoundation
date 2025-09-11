package com.wty.foundation.common.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 反射工具类，提供获取泛型类型信息的相关方法
 *
 * <p>该类包含获取对象泛型类型和ViewModel泛型类型的工具方法，
 * 用于在反射场景下解析类的泛型参数信息。
 */
public class ReflectionUtils {
    /**
     * 私有构造函数，防止外部实例化该工具类
     */
    private ReflectionUtils() {
    }

    /**
     * 根据对象和索引获取其泛型父类的指定位置泛型参数类型
     *
     * <p>该方法通过反射获取对象所属类的泛型父类，并返回指定索引位置的泛型参数类型。
     * 适用于需要在运行时获取类定义的泛型类型信息的场景。
     *
     * @param obj   用于获取其泛型信息的对象，不能为null
     * @param index 泛型参数的索引位置，从0开始
     * @param <T>   泛型参数的类型
     * @return 指定索引位置的泛型参数对应的Class对象
     * @throws NullPointerException     如果传入的obj为null
     * @throws IllegalArgumentException 如果该类未使用泛型参数，或指定索引的泛型参数不存在
     */
    public static @NonNull <T> Class<T> getClass(Object obj, int index) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Type type = obj.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            type = ArrayUtils.get(((ParameterizedType) type).getActualTypeArguments(), index);
        } else {
            throw new IllegalArgumentException("该类未使用泛型参数");
        }
        if (type == null) {
            throw new IllegalArgumentException("指定索引的泛型参数不存在");
        }
        return (Class<T>) type;
    }

    /**
     * 获取指定类及其父类中定义的ViewModel泛型参数类型
     *
     * <p>该方法通过递归遍历类的继承链，查找定义的ViewModel类型泛型参数，
     * 适用于从Activity或Fragment等类中解析其关联的ViewModel类型。
     *
     * @param clazz 需要获取ViewModel泛型信息的类，通常是Activity或Fragment的Class对象
     * @param <VB>  ViewModel的子类类型
     * @return 找到的ViewModel子类对应的Class对象
     * @throws IllegalArgumentException 如果在类的继承链中未找到ViewModel的泛型参数
     */
    public static <VB extends ViewModel> Class<VB> getVMClass(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            if (genericSuperclass instanceof ParameterizedType) {
                for (Type type : ((ParameterizedType) genericSuperclass).getActualTypeArguments()) {
                    if (type instanceof Class && ViewModel.class.isAssignableFrom((Class) type)) {
                        try {
                            return (Class<VB>) type;
                        } catch (Exception e) {
                            Log.i("ViewBindingUtils", Log.getStackTraceString(e));
                        }
                    }
                }
            }
            genericSuperclass = superclass.getGenericSuperclass();
            superclass = superclass.getSuperclass();
        }
        throw new IllegalArgumentException("没有找到ViewModel的泛型参数");
    }
}