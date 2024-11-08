package com.wty.foundation.common.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.wty.foundation.BuildConfig;

import android.util.Log;

/**
 * 日志工具类，提供了一系列静态方法来记录不同级别的日志
 * 该工具类考虑了多线程环境下的安全性，并提供了丰富的日志信息，包括线程名、类名、方法名和行号
 * 在发布版本中可以通过 {@link BuildConfig#DEBUG} 来控制是否打印日志
 */
public class LogUtils {
    private static final String DEFAULT_TAG = "LogUtils"; // 默认日志标签
    private static final int MAX_LOG_SIZE = 4000; // Android最大单条日志大小
    private static final ThreadLocal<String> CURRENT_TAG = new ThreadLocal<>(); // 当前线程的日志标签
    private static final ThreadLocal<Boolean> INCLUDE_STACK_TRACE = new ThreadLocal<>(); // 是否包含堆栈跟踪信息
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault()); // 日期格式
    private static final SimpleDateFormat DATE_TIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()); // 日期时间格式

    private static final BlockingQueue<Runnable> LOG_QUEUE = new LinkedBlockingQueue<>(); // 日志任务队列
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, // 核心线程数
        5, // 最大线程数
        60L, // 空闲线程存活时间
        TimeUnit.SECONDS, // 时间单位
        LOG_QUEUE // 工作队列
    );

    static {
        EXECUTOR.prestartAllCoreThreads(); // 预启动所有核心线程
    }

    private static String LOG_FILE_PATH = ""; // 初始为空的日志文件路径
    private static final String LOG_TAG = "LogUtils"; // 日志工具类的标签
    private static volatile int currentLogLevel = Log.DEBUG; // 默认日志级别
    private static int successCount = 0; // 成功记录的日志数量
    private static int failureCount = 0; // 失败记录的日志数量
    private static LogCallback logCallback = null; // 日志回调接口
    private static long lastCallbackTime = 0; // 上次调用回调的时间
    private static final long CALLBACK_THROTTLE_PERIOD = 1000; // 至少每秒调用一次回调
    private static final long LOG_RETENTION_PERIOD = 7 * 24 * 60 * 60 * 1000L; // 日志保留周期（7天）
    private static final Queue<String> pendingLogs = new LinkedList<>(); // 待处理日志队列
    private static final int MAX_PENDING_LOGS = 1000; // 待处理日志的最大数量
    private static final long MAX_LOG_WRITE_DURATION = 500; // 最大日志写入耗时（毫秒）

    /**
     * 动态设置日志文件路径
     *
     * @param baseDir 日志文件的基础路径，例如："/storage/emulated/0/Android/data/com.example.myapp/files/logs/"
     */
    public static void setLogFilepath(String baseDir) {
        if (baseDir == null || baseDir.isEmpty() || baseDir.contains("..")) {
            Log.e(LOG_TAG, "Invalid log file path: " + baseDir);
            return;
        }
        String logFileName = "app_" + DATE_FORMAT.format(new Date()) + ".log";
        LOG_FILE_PATH = new File(baseDir, logFileName).getAbsolutePath();
        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to create log file", e);
            }
        }
    }

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
     * 设置是否包含堆栈跟踪信息
     *
     * @param include 是否包含堆栈跟踪信息
     */
    public static void setIncludeStackTrace(boolean include) {
        INCLUDE_STACK_TRACE.set(include);
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
        if (INCLUDE_STACK_TRACE.get() == null || !INCLUDE_STACK_TRACE.get()) {
            return msg;
        }

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
        if (currentLogLevel <= Log.VERBOSE) {
            logAsync(Log.VERBOSE, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录DEBUG级别的日志
     *
     * @param msg 日志消息
     */
    public static void d(String msg) {
        if (currentLogLevel <= Log.DEBUG) {
            logAsync(Log.DEBUG, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录INFO级别的日志
     *
     * @param msg 日志消息
     */
    public static void i(String msg) {
        if (currentLogLevel <= Log.INFO) {
            logAsync(Log.INFO, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录WARN级别的日志
     *
     * @param msg 日志消息
     */
    public static void w(String msg) {
        if (currentLogLevel <= Log.WARN) {
            logAsync(Log.WARN, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录ERROR级别的日志
     *
     * @param msg 日志消息
     */
    public static void e(String msg) {
        if (currentLogLevel <= Log.ERROR) {
            logAsync(Log.ERROR, getCurrentTag(), createLogMessage(msg));
        }
    }

    /**
     * 记录ERROR级别的日志，并附带异常信息
     *
     * @param msg 日志消息
     * @param tr 异常对象
     */
    public static void e(String msg, Throwable tr) {
        if (currentLogLevel <= Log.ERROR) {
            logAsync(Log.ERROR, getCurrentTag(), createLogMessage(msg), tr);
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
    private static void logAsync(int priority, String tag, String message, Throwable tr) {
        EXECUTOR.execute(() -> {
            log(priority, tag, message);
            log(priority, tag, Log.getStackTraceString(tr));
            writeToFile(message + "\n" + Log.getStackTraceString(tr));
        });
    }

    /**
     * 记录日志消息到文件
     *
     * @param message 日志消息
     */
    private static void writeToFile(String message) {
        long startTime = System.currentTimeMillis();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = DATE_TIME_FORMAT.format(new Date());
            String filteredMessage = filterLogContent(message);
            writer.write(String.format("[%s] %s\n", timestamp, filteredMessage));
            successCount++;
            invokeLogCallback(true);
        } catch (IOException e) {
            failureCount++;
            invokeLogCallback(false);
            // 添加到待处理日志队列
            synchronized (pendingLogs) {
                if (pendingLogs.size() >= MAX_PENDING_LOGS) {
                    handlePendingLogsFull();
                } else {
                    pendingLogs.add(message);
                }
            }
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logWriteDuration(startTime, endTime);
            if (duration > MAX_LOG_WRITE_DURATION) {
                triggerPerformanceAlert(duration);
            }
            rotateAndCleanupLogs();
            cleanupOldLogs();
        }
    }

    /**
     * 异步记录日志
     *
     * @param priority 日志级别
     * @param tag 日志标签
     * @param message 日志消息
     */
    private static void logAsync(int priority, String tag, String message) {
        EXECUTOR.execute(() -> {
            log(priority, tag, message);
            writeToFile(message);
        });
    }

    /**
     * 清除当前线程的日志标签
     */
    public static void clearCurrentTag() {
        CURRENT_TAG.remove();
    }

    /**
     * 清除当前线程的堆栈跟踪标志
     */
    public static void clearStackTraceFlag() {
        INCLUDE_STACK_TRACE.remove();
    }

    /**
     * 设置当前日志级别
     *
     * @param level 日志级别
     */
    public static void setLogLevel(int level) {
        currentLogLevel = level;
    }

    /**
     * 获取当前日志级别
     *
     * @return 当前日志级别
     */
    public static int getLogLevel() {
        return currentLogLevel;
    }

    /**
     * 记录日志写入耗时
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    private static void logWriteDuration(long startTime, long endTime) {
        long duration = endTime - startTime;
        Log.d(LOG_TAG, "Log write duration: " + duration + " ms");
    }

    /**
     * 触发性能警报
     *
     * @param duration 日志写入耗时
     */
    private static void triggerPerformanceAlert(long duration) {
        synchronized (LogUtils.class) {
            if (logCallback != null) {
                EXECUTOR.execute(() -> {
                    logCallback.onPerformanceAlert(duration);
                });
            }
        }
    }

    /**
     * 进行日志内容过滤
     *
     * @param message 日志消息
     * @return 过滤后的日志消息
     */
    private static String filterLogContent(String message) {
        // 示例：过滤掉敏感信息
        return message.replaceAll("(password|token)=.*?(\\s|$)", "$1=***$2");
    }

    /**
     * 日志文件轮转和清理
     */
    private static synchronized void rotateAndCleanupLogs() {
        File logFile = new File(LOG_FILE_PATH);
        if (logFile.exists() && logFile.length() > 1024 * 1024 * 5) { // 5MB
            File newLogFile = new File(LOG_FILE_PATH + ".new");
            try {
                newLogFile.createNewFile();
                String backupFilePath = LOG_FILE_PATH + ".bak";
                File backupFile = new File(backupFilePath);
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                logFile.renameTo(backupFile);
                newLogFile.renameTo(logFile); // 将临时文件重命名为正式日志文件
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to rotate log file", e);
            }
        }
    }

    /**
     * 定期清理旧的日志文件
     */
    private static void cleanupOldLogs() {
        File logDir = new File(LOG_FILE_PATH).getParentFile();
        if (logDir != null && logDir.exists()) {
            File[] files = logDir.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".log.bak"));
            if (files != null) {
                for (File file : files) {
                    if (System.currentTimeMillis() - file.lastModified() > LOG_RETENTION_PERIOD) {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * 处理日志缓冲区已满的情况
     */
    private static synchronized void handlePendingLogsFull() {
        // 降低日志级别
        if (currentLogLevel <= Log.DEBUG) {
            setLogLevel(Log.WARN);
            Log.w(LOG_TAG, "Pending logs queue is full, degrading log level to WARN.");
        }

        // 丢弃最旧的日志
        while (pendingLogs.size() >= MAX_PENDING_LOGS) {
            pendingLogs.poll();
        }

        // 通知用户
        if (logCallback != null) {
            EXECUTOR.execute(() -> {
                logCallback.onPendingLogsFull(MAX_PENDING_LOGS);
            });
        }
    }

    /**
     * 设置日志回调
     *
     * @param callback 日志回调
     */
    public static synchronized void setLogCallback(LogCallback callback) {
        logCallback = callback;
    }

    /**
     * 调用日志回调
     *
     * @param success 是否成功
     */
    private static synchronized void invokeLogCallback(boolean success) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallbackTime > CALLBACK_THROTTLE_PERIOD) {
            if (logCallback != null) {
                EXECUTOR.execute(() -> {
                    synchronized (LogUtils.class) {
                        if (success) {
                            logCallback.onSuccess(successCount);
                        } else {
                            logCallback.onError(failureCount);
                        }
                    }
                });
            }
            lastCallbackTime = currentTime;
        }
    }

    /**
     * 关闭线程池
     */
    public static void shutdownExecutor() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
        }
    }

    /**
     * 日志回调接口
     */
    public interface LogCallback {
        void onSuccess(int successCount);

        void onError(int failureCount);

        void onPendingLogsFull(int maxPendingLogs);

        void onPerformanceAlert(long duration);
    }
}