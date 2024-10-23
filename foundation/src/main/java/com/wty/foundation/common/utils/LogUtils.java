package com.wty.foundation.common.utils;

import com.wty.foundation.BuildConfig;

import android.util.Log;

/**
 * 日志工具类，提供了一系列静态方法来记录不同级别的日志
 * 该工具类考虑了多线程环境下的安全性，并提供了丰富的日志信息，包括线程名、类名、方法名和行号
 * 在发布版本中可以通过 {@link BuildConfig#DEBUG} 来控制是否打印日志
 */
public class LogUtils {

    private static final String DEFAULT_TAG = "LogUtils";
    private static final int MAX_LOG_SIZE = 4000; // Android最大单条日志大小
    private static final ThreadLocal<String> CURRENT_TAG = new ThreadLocal<>();

    /**
     * 检查当前构建类型是否为调试模式
     *
     * @return 如果是调试模式返回true，否则返回false
     */
    public static boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    /**
     * 设置当前线程的日志标签
     *
     * @param tag 日志标签
     */
    public static void setCurrentTag(String tag) {
        if (tag != null) {
            CURRENT_TAG.set(tag);
        }
    }

    /**
     * 获取当前线程的日志标签，如果未设置则使用默认标签
     *
     * @return 当前线程的日志标签
     */
    private static String getCurrentTag() {
        String tag = CURRENT_TAG.get();
        return (tag != null) ? tag : DEFAULT_TAG;
    }

    /**
     * 创建带有时间戳、线程信息、类名、方法名和行号的日志消息
     *
     * @param msg 原始日志消息
     * @return 格式化后的日志消息
     */
    private static String createLogMessage(String msg) {
        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            if (stackTraceElements.length < 4) {
                return msg;
            }
            StackTraceElement targetElement = stackTraceElements[3];
            String className = targetElement.getClassName();
            String methodName = targetElement.getMethodName();
            int lineNumber = targetElement.getLineNumber();

            return String.format("[Thread: %s] %s.%s(%d): %s", Thread.currentThread().getName(), className, methodName,
                lineNumber, msg);
        } catch (Exception e) {
            // 防止堆栈跟踪解析失败导致应用崩溃
            return msg;
        }
    }

    /**
     * 分割过长的日志消息并打印
     *
     * @param priority 日志级别
     * @param tag 日志标签
     * @param message 日志消息
     */
    private static void log(int priority, String tag, String message) {
        if (message.length() > MAX_LOG_SIZE) {
            for (int i = 0; i <= message.length() / MAX_LOG_SIZE; i++) {
                int start = i * MAX_LOG_SIZE;
                int end = (i + 1) * MAX_LOG_SIZE;
                end = end < message.length() ? end : message.length();
                Log.println(priority, tag, message.substring(start, end));
            }
        } else {
            Log.println(priority, tag, message);
        }
    }

    /**
     * 记录VERBOSE级别的日志
     *
     * @param msg 日志消息
     */
    public static void v(String msg) {
        if (isDebug()) {
            log(Log.VERBOSE, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录DEBUG级别的日志
     *
     * @param msg 日志消息
     */
    public static void d(String msg) {
        if (isDebug()) {
            log(Log.DEBUG, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录INFO级别的日志
     *
     * @param msg 日志消息
     */
    public static void i(String msg) {
        if (isDebug()) {
            log(Log.INFO, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录WARN级别的日志
     *
     * @param msg 日志消息
     */
    public static void w(String msg) {
        if (isDebug()) {
            log(Log.WARN, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录ERROR级别的日志
     *
     * @param msg 日志消息
     */
    public static void e(String msg) {
        if (isDebug()) {
            log(Log.ERROR, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录ERROR级别的日志，并附带异常信息
     *
     * @param msg 日志消息
     * @param tr 异常对象
     */
    public static void e(String msg, Throwable tr) {
        if (isDebug()) {
            log(Log.ERROR, getCurrentTag(), createLogMessage(msg), tr);
        }
    }

    /**
     * 记录带有异常信息的ERROR级别的日志
     *
     * @param priority 日志级别
     * @param tag 日志标签
     * @param message 日志消息
     * @param tr 异常对象
     */
    private static void log(int priority, String tag, String message, Throwable tr) {
        if (message.length() > MAX_LOG_SIZE) {
            for (int i = 0; i <= message.length() / MAX_LOG_SIZE; i++) {
                int start = i * MAX_LOG_SIZE;
                int end = (i + 1) * MAX_LOG_SIZE;
                end = end < message.length() ? end : message.length();
                Log.println(priority, tag, message.substring(start, end));
            }
        } else {
            Log.println(priority, tag, message);
        }
        Log.println(priority, tag, Log.getStackTraceString(tr));
    }

    /**
     * 清除当前线程的日志标签
     */
    public static void clearCurrentTag() {
        CURRENT_TAG.remove();
    }
}