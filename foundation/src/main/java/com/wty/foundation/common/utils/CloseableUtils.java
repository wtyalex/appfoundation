package com.wty.foundation.common.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloseableUtils {

    private static final Logger LOGGER = Logger.getLogger(CloseableUtils.class.getName());
    private static final String TAG = "CloseableUtils";

    // 私有构造函数防止外部实例化
    private CloseableUtils() {}

    /**
     * 安全关闭 Closeable 对象。 如果对象不是 null，则尝试关闭它，并捕获任何可能发生的 IOException。
     *
     * @param closeable 要关闭的对象
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logError(e);
            }
        }
    }

    /**
     * 尝试关闭多个 Closeable 对象。 该方法接受可变参数列表的 Closeable 对象，并尝试依次关闭它们。 如果某个对象关闭时抛出异常，异常会被记录但不会阻止其他对象的关闭。
     *
     * @param closeables 要关闭的对象列表
     */
    public static void closeAll(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            close(closeable);
        }
    }

    /**
     * 尝试关闭 AutoCloseable 实现对象。 该方法用于支持 Java 7 及以上版本的 try-with-resources 语法中的资源。
     *
     * @param autoCloseable 要关闭的对象
     */
    public static void close(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                logError(e);
            }
        }
    }

    /**
     * 尝试关闭多个 AutoCloseable 实现对象。 该方法接受可变参数列表的 AutoCloseable 对象，并尝试依次关闭它们。 如果某个对象关闭时抛出异常，异常会被记录但不会阻止其他对象的关闭。
     *
     * @param autoCloseables 要关闭的对象列表
     */
    public static void closeAll(AutoCloseable... autoCloseables) {
        for (AutoCloseable autoCloseable : autoCloseables) {
            close(autoCloseable);
        }
    }

    /**
     * 批量关闭多个资源，并捕获所有发生的异常。 如果有异常发生，将收集所有的异常，并最终抛出一个 CompositeException 包含所有异常。
     *
     * @param closeables 要关闭的资源列表
     * @throws CompositeException 如果有多个异常发生，则抛出包含所有异常的 CompositeException
     */
    public static void closeAllWithExceptions(List<Closeable> closeables) throws CompositeException {
        List<IOException> exceptions = new ArrayList<>();
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            throw new CompositeException(exceptions);
        }
    }

    /**
     * 忽略特定类型的异常。 在关闭资源时，如果发生指定类型的异常，则忽略并继续。
     *
     * @param closeable 要关闭的资源
     * @param ignoreExceptionType 要忽略的异常类型
     */
    public static void closeIgnoring(Class<? extends Exception> ignoreExceptionType, Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (ignoreExceptionType.isInstance(e)) {
                    // Ignore the exception
                } else {
                    logError(e);
                }
            }
        }
    }

    /**
     * 关闭 AutoCloseable 资源时忽略特定类型的异常。
     *
     * @param autoCloseable 要关闭的资源
     * @param ignoreExceptionType 要忽略的异常类型
     */
    public static void closeIgnoring(Class<? extends Exception> ignoreExceptionType, AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                if (ignoreExceptionType.isInstance(e)) {
                    // Ignore the exception
                } else {
                    logError(e);
                }
            }
        }
    }

    /**
     * 关闭资源时尝试重试。 如果关闭失败，可以指定最大重试次数。
     *
     * @param closeable 要关闭的资源
     * @param maxRetries 最大重试次数
     */
    public static void closeWithRetry(Closeable closeable, int maxRetries) {
        int retries = 0;
        while (retries <= maxRetries) {
            try {
                closeable.close();
                break;
            } catch (IOException e) {
                retries++;
                if (retries > maxRetries) {
                    logError(e);
                    break;
                }
                try {
                    Thread.sleep(100); // Simple backoff strategy.
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 记录错误日志。
     *
     * @param e 异常对象
     */
    private static void logError(Exception e) {
        LOGGER.log(Level.SEVERE, TAG + ": An error occurred while closing resources.", e);
    }

    /**
     * 一个包含多个异常的复合异常类。
     */
    public static class CompositeException extends Exception {
        private final List<IOException> exceptions;

        public CompositeException(List<IOException> exceptions) {
            super("Multiple exceptions occurred while closing resources.");
            this.exceptions = exceptions;
        }

        /**
         * 获取所有收集的异常。
         *
         * @return 异常列表
         */
        public List<IOException> getExceptions() {
            return exceptions;
        }
    }
}
