package com.wty.foundation.core.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

/**
 * @author wutianyu
 * @createTime 2023/6/6 10:54
 * @describe ViewBinding工具类，提供通过反射创建ViewBinding实例的功能
 */
public class ViewBindingUtils {

    public static <Binding extends ViewBinding> Binding create(Class<?> clazz, LayoutInflater inflater) {
        return create(clazz, inflater, null);
    }

    public static <Binding extends ViewBinding> Binding create(Class<?> clazz, LayoutInflater inflater,
        ViewGroup root) {
        return create(clazz, inflater, root, false);
    }

    @NonNull
    public static <Binding extends ViewBinding> Binding create(Class<?> clazz, LayoutInflater inflater, ViewGroup root,
        boolean attachToRoot) {
        Class<?> bindingClass = getBindingClass(clazz);
        Binding binding = null;
        if (bindingClass != null) {
            try {
                Method method = bindingClass.getMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);
                binding = (Binding)method.invoke(null, inflater, root, attachToRoot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Objects.requireNonNull(binding);
    }

    private static <VB extends ViewBinding> Class<?> getBindingClass(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            if (genericSuperclass instanceof ParameterizedType) {
                for (Type type : ((ParameterizedType)genericSuperclass).getActualTypeArguments()) {
                    if (type instanceof Class && ViewBinding.class.isAssignableFrom((Class)type)) {
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
