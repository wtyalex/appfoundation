package com.wty.foundation.common.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

public class ReflectionUtils {
    private ReflectionUtils() {}

    public static @NonNull <T> Class<T> getClass(Object obj, int index) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Type type = obj.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            type = ArrayUtils.get(((ParameterizedType)type).getActualTypeArguments(), index);
        } else {
            throw new IllegalArgumentException();
        }
        if (type == null) {
            throw new IllegalArgumentException();
        }
        return (Class<T>)type;
    }

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
        throw new IllegalArgumentException("There is no generic of ViewBinding.");
    }
}
