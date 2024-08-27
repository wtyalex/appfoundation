package com.wty.foundation.common.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

public class ReflectionUtils {
    // 私有构造函数防止外部实例化
    private ReflectionUtils() {}

    // 根据对象和索引获取泛型类型
    // obj: 用于获取其泛型信息的对象
    // index: 泛型参数的索引位置
    public static @NonNull <T> Class<T> getClass(Object obj, int index) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Type type = obj.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            type = ArrayUtils.get(((ParameterizedType)type).getActualTypeArguments(), index);
        } else {
            throw new IllegalArgumentException("该类未使用泛型参数");
        }
        if (type == null) {
            throw new IllegalArgumentException("指定索引的泛型参数不存在");
        }
        return (Class<T>)type;
    }

    // 获取ViewModel的泛型类型
    // clazz: 需要获取ViewModel泛型信息的类
    public static <VB extends ViewModel> Class<VB> getVMClass(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            if (genericSuperclass instanceof ParameterizedType) {
                for (Type type : ((ParameterizedType)genericSuperclass).getActualTypeArguments()) {
                    if (type instanceof Class && ViewModel.class.isAssignableFrom((Class)type)) {
                        try {
                            return (Class<VB>)type;
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
