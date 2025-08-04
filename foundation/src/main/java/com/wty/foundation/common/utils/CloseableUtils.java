package com.wty.foundation.common.utils;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 关闭资源的工具类，提供了多种关闭 Closeable 和 AutoCloseable 资源的方法
 */
public class CloseableUtils {
    private static final String TAG = "CloseableUtils";

    // 私有构造函数，防止外部实例化该工具类
    private CloseableUtils() {
    }

    /**
     * 关闭单个 Closeable 资源
     *
     * @param closeable 要关闭的 Closeable 资源，若为 null 则不执行操作
     */
    public static void close(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            logError("Closing Closeable failed", e);
        }
    }

    /**
     * 关闭多个 Closeable 资源
     *
     * @param closeables 要关闭的 Closeable 资源数组，若为 null 则不执行操作
     */
    public static void closeAll(Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable c : closeables) close(c);
    }

    /**
     * 关闭单个 AutoCloseable 资源
     *
     * @param autoCloseable 要关闭的 AutoCloseable 资源，若为 null 则不执行操作
     */
    public static void close(AutoCloseable autoCloseable) {
        if (autoCloseable == null) return;
        try {
            autoCloseable.close();
        } catch (Exception e) {
            logError("Closing AutoCloseable failed", e);
        }
    }

    /**
     * 关闭多个 AutoCloseable 资源
     *
     * @param autoCloseables 要关闭的 AutoCloseable 资源数组，若为 null 则不执行操作
     */
    public static void closeAll(AutoCloseable... autoCloseables) {
        if (autoCloseables == null) return;
        for (AutoCloseable ac : autoCloseables) close(ac);
    }

    /**
     * 关闭 Closeable 资源，忽略指定类型的异常
     *
     * @param closeable            要关闭的 Closeable 资源，若为 null 则不执行操作
     * @param ignoredExceptionType 要忽略的异常类型，若为 null 则不执行操作
     */
    public static void closeIgnoring(Closeable closeable, Class<? extends Exception> ignoredExceptionType) {
        if (closeable == null || ignoredExceptionType == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            if (!ignoredExceptionType.isInstance(e)) {
                logError("Closing Closeable failed", e);
            }
        }
    }

    /**
     * 尝试多次关闭 Closeable 资源
     *
     * @param closeable  要关闭的 Closeable 资源，若为 null 则不执行操作
     * @param maxRetries 最大重试次数，若小于 0 则不执行操作
     */
    public static void closeWithRetry(Closeable closeable, int maxRetries) {
        if (closeable == null || maxRetries < 0) return;

        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                closeable.close();
                return;
            } catch (IOException e) {
                if (attempts++ == maxRetries) {
                    logError("Close failed after " + maxRetries + " retries", e);
                }
                sleepQuietly(100);
            }
        }
    }

    /**
     * 线程安静休眠指定毫秒数，忽略中断异常
     *
     * @param millis 要休眠的毫秒数
     */
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 记录错误日志
     *
     * @param message 错误消息
     * @param e       异常对象
     */
    private static void logError(String message, Throwable e) {
        Log.e(TAG, message, e);
    }

    /**
     * 关闭混合的 Closeable 和 AutoCloseable 资源列表
     *
     * @param resources 资源列表，若为 null 或包含 null 元素则跳过对应元素
     */
    public static void closeAllMixed(List<?> resources) {
        if (resources == null) return;
        for (Object obj : resources) {
            if (obj instanceof Closeable) {
                close((Closeable) obj);
            } else if (obj instanceof AutoCloseable) {
                close((AutoCloseable) obj);
            }
        }
    }

    /**
     * 安全执行操作并在最后关闭 Closeable 资源
     *
     * @param closeable    要关闭的 Closeable 资源
     * @param action       要执行的操作
     * @param defaultValue 操作失败时的默认返回值
     * @return 操作结果，若失败则返回默认值
     */
    public static <T> T executeSafely(Closeable closeable, SafeClosure<T> action, T defaultValue) {
        try {
            return action.execute(closeable);
        } catch (Exception e) {
            logError("Safe execute failed", e);
            return defaultValue;
        } finally {
            close(closeable);
        }
    }

    public interface SafeClosure<T> {
        /**
         * 执行操作
         *
         * @param closeable 关联的 Closeable 资源
         * @return 操作结果
         * @throws Exception 操作过程中可能抛出的异常
         */
        T execute(Closeable closeable) throws Exception;
    }
}