package com.wty.foundation.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wty.foundation.BuildConfig;
import com.wty.foundation.common.init.AppContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志工具类，用于管理日志记录、文件存储、日志级别控制等功能
 */
public class LogUtils {
    private static final String TAG = "LogUtils";
    // 日志最大长度
    private static final int MAX_LOG_LENGTH = 4000;
    // 日志刷新间隔（毫秒）
    private static final int LOG_FLUSH_INTERVAL = 2000;
    // 日志文件最大大小（字节）
    private static final int MAX_LOG_FILE_SIZE = 5 * 1024 * 1024;
    // 日志保留时长（毫秒）
    private static final long LOG_RETENTION_DAYS = 7 * 24 * 60 * 60 * 1000L;
    // 成功写入日志的计数
    private static final AtomicInteger successCount = new AtomicInteger(0);
    // 写入日志失败的计数
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    // 日志文件路径
    private static volatile String logFilePath = null;
    // 当前日志级别
    private static volatile int currentLogLevel = BuildConfig.DEBUG ? Log.VERBOSE : Log.INFO;
    // 应用上下文
    private static volatile Context appContext;
    // 线程特定的日志标签
    private static final ThreadLocal<String> threadSpecificTag = new ThreadLocal<>();
    // 是否包含堆栈跟踪信息
    private static final ThreadLocal<Boolean> includeStackTrace = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    // 日期格式化器
    private static final ThreadLocal<SimpleDateFormat> dateFormatHolder = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        }
    };
    // 日志写入线程
    private static final HandlerThread logHandlerThread = new HandlerThread("LogWriter", Process.THREAD_PRIORITY_BACKGROUND);

    static {
        // 启动日志写入线程
        logHandlerThread.start();
    }

    // 日志处理的Handler
    private static final Handler logHandler = new Handler(logHandlerThread.getLooper());
    // 日志缓冲区
    private static final BlockingQueue<String> logBuffer = new LinkedBlockingQueue<>(1000);
    // 主线程的Handler
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 全局日志回调
    private static volatile LogCallback globalLogCallback;

    /**
     * 构造函数，初始化应用上下文
     */
    public LogUtils() {
        if (appContext == null) {
            synchronized (LogUtils.class) {
                if (appContext == null) {
                    appContext = AppContext.getInstance().getContext();
                }
            }
        }
    }

    /**
     * 设置自定义日志目录
     *
     * @param customDir 自定义日志目录
     */
    public static synchronized void setCustomLogDir(@NonNull File customDir) {
        if (!customDir.canWrite()) {
            Log.e(TAG, "Directory not writable: " + customDir);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 若目录不存在则创建
            if (!customDir.isDirectory() && !customDir.mkdirs()) {
                Log.e(TAG, "Failed to create custom log directory");
                logFilePath = null;
                return;
            }
        }

        // 生成日志文件名
        String fileName = "app_" + getCurrentDate() + ".log";
        logFilePath = new File(customDir, fileName).getAbsolutePath();
        // 安排日志刷新任务
        scheduleFlushTask();
        // 安排日志清理任务
        scheduleCleanupTask();
    }

    /**
     * 禁用文件日志记录
     */
    public static void disableFileLog() {
        logFilePath = null;
        // 移除刷新和清理任务
        logHandler.removeCallbacks(flushTask);
        logHandler.removeCallbacks(cleanupTask);
    }

    /**
     * 记录VERBOSE级别的日志
     *
     * @param msg 日志消息
     */
    public static void v(String msg) {
        log(Log.VERBOSE, msg, null);
    }

    /**
     * 记录DEBUG级别的日志
     *
     * @param msg 日志消息
     */
    public static void d(String msg) {
        log(Log.DEBUG, msg, null);
    }

    /**
     * 记录INFO级别的日志
     *
     * @param msg 日志消息
     */
    public static void i(String msg) {
        log(Log.INFO, msg, null);
    }

    /**
     * 记录WARN级别的日志
     *
     * @param msg 日志消息
     */
    public static void w(String msg) {
        log(Log.WARN, msg, null);
    }

    /**
     * 记录ERROR级别的日志
     *
     * @param msg 日志消息
     */
    public static void e(String msg) {
        log(Log.ERROR, msg, null);
    }

    /**
     * 记录带异常的ERROR级别的日志
     *
     * @param msg 日志消息
     * @param tr  异常信息
     */
    public static void e(String msg, Throwable tr) {
        log(Log.ERROR, msg, tr);
    }

    /**
     * 设置线程特定的日志标签
     *
     * @param tag 日志标签
     */
    public static void setThreadTag(String tag) {
        threadSpecificTag.set(tag);
    }

    /**
     * 清除线程特定的日志标签
     */
    public static void clearThreadTag() {
        threadSpecificTag.remove();
    }

    /**
     * 设置是否包含堆栈跟踪信息
     *
     * @param include 是否包含
     */
    public static void setIncludeStackTrace(boolean include) {
        includeStackTrace.set(include);
    }

    /**
     * 设置日志级别
     *
     * @param level 日志级别
     */
    public static void setLogLevel(int level) {
        currentLogLevel = level;
    }

    /**
     * 设置全局日志回调
     *
     * @param callback 日志回调
     */
    public static void setLogCallback(@Nullable LogCallback callback) {
        globalLogCallback = callback;
    }

    /**
     * 刷新日志缓冲区并关闭日志写入线程
     */
    public static void flushAndShutdown() {
        // 移除所有任务和消息
        logHandler.removeCallbacksAndMessages(null);
        logHandler.post(() -> {
            // 刷新缓冲区到文件
            flushBufferToFile();
            // 安全退出日志写入线程
            logHandlerThread.quitSafely();
        });
    }

    /**
     * 记录日志
     *
     * @param priority 日志级别
     * @param msg      日志消息
     * @param tr       异常信息
     */
    private static void log(int priority, String msg, @Nullable Throwable tr) {
        // 若日志级别低于当前设置级别则不记录
        if (priority < currentLogLevel) return;

        // 获取有效的日志标签
        final String tag = getEffectiveTag();
        // 构建完整的日志消息
        final String fullMessage = buildLogMessage(msg, tr);

        // 输出到控制台
        doConsoleLog(priority, tag, fullMessage);

        // 若日志文件路径存在则入队到缓冲区
        if (logFilePath != null && !logFilePath.isEmpty()) {
            enqueueLog(fullMessage);
        }
    }

    /**
     * 将日志消息加入缓冲区
     *
     * @param message 日志消息
     */
    private static void enqueueLog(String message) {
        // 若缓冲区已满则处理
        if (!logBuffer.offer(message)) {
            handleBufferFull(message);
        }
    }

    /**
     * 将日志输出到控制台
     *
     * @param priority 日志级别
     * @param tag      日志标签
     * @param message  日志消息
     */
    private static void doConsoleLog(int priority, String tag, String message) {
        if (message.length() <= MAX_LOG_LENGTH) {
            Log.println(priority, tag, message);
            return;
        }

        // 若消息过长则分段输出
        for (int i = 0, len = message.length(); i < len; i += MAX_LOG_LENGTH) {
            int end = Math.min(i + MAX_LOG_LENGTH, len);
            Log.println(priority, tag, message.substring(i, end));
        }
    }

    /**
     * 获取有效的日志标签
     *
     * @return 日志标签
     */
    private static String getEffectiveTag() {
        String customTag = threadSpecificTag.get();
        return customTag != null ? customTag : TAG;
    }

    /**
     * 构建完整的日志消息
     *
     * @param msg 日志消息
     * @param tr  异常信息
     * @return 完整的日志消息
     */
    private static String buildLogMessage(String msg, @Nullable Throwable tr) {
        StringBuilder sb = new StringBuilder(256);
        // 添加时间和线程名
        sb.append(dateFormatHolder.get().format(new Date())).append(" [").append(Thread.currentThread().getName()).append("] ");

        // 若需要则添加堆栈跟踪信息
        if (includeStackTrace.get()) {
            appendStackTrace(sb);
        }

        sb.append(msg);

        // 若有异常则添加异常堆栈信息
        if (tr != null) {
            sb.append('\n').append(Log.getStackTraceString(tr));
        }

        // 清理消息中的敏感信息
        return sanitizeMessage(sb.toString());
    }

    /**
     * 添加堆栈跟踪信息到日志消息
     *
     * @param sb 日志消息构建器
     */
    private static void appendStackTrace(StringBuilder sb) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().equals(LogUtils.class.getName())) continue;
            if (element.getClassName().startsWith("android.util.Log")) continue;

            sb.append(element.getClassName()).append(".").append(element.getMethodName()).append(":").append(element.getLineNumber());
            break;
        }
        sb.append(" - ");
    }

    /**
     * 清理日志消息中的敏感信息
     *
     * @param message 日志消息
     * @return 清理后的日志消息
     */
    private static String sanitizeMessage(String message) {
        return message.replaceAll("(password|token|auth)=[^&]+", "$1=***");
    }

    // 日志刷新任务
    private static final Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            // 刷新缓冲区到文件
            flushBufferToFile();
            // 重新安排刷新任务
            scheduleFlushTask();
        }
    };

    /**
     * 安排日志刷新任务
     */
    private static void scheduleFlushTask() {
        logHandler.postDelayed(flushTask, LOG_FLUSH_INTERVAL);
    }

    /**
     * 刷新日志缓冲区到文件
     */
    private static synchronized void flushBufferToFile() {
        if (logFilePath == null || logFilePath.isEmpty() || logBuffer.isEmpty()) return;

        try {
            File logFile = new File(logFilePath);
            // 若文件大小超过限制则轮转日志文件
            if (logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile(logFile);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                while (!logBuffer.isEmpty()) {
                    String log = logBuffer.poll();
                    if (log != null) {
                        writer.write(log);
                        writer.newLine();
                        // 成功计数加1
                        successCount.incrementAndGet();
                    }
                }
                // 通知日志写入成功
                notifySuccess();
            }
        } catch (IOException e) {
            // 失败计数加1
            failureCount.incrementAndGet();
            // 恢复失败的日志
            recoverFailedLogs();
            // 通知日志写入失败
            notifyFailure();
        }
    }

    /**
     * 轮转日志文件
     *
     * @param currentFile 当前日志文件
     */
    private static void rotateLogFile(File currentFile) {
        String backupPath = currentFile.getAbsolutePath() + ".bak";
        File backupFile = new File(backupPath);

        // 若备份文件存在则删除
        if (backupFile.exists() && !backupFile.delete()) {
            Log.w(TAG, "Failed to delete old backup: " + backupPath);
        }

        // 重命名当前日志文件为备份文件
        if (!currentFile.renameTo(backupFile)) {
            Log.w(TAG, "Failed to rotate log file");
        }
    }

    /**
     * 恢复失败的日志
     */
    private static void recoverFailedLogs() {
        LinkedBlockingQueue<String> retryQueue = new LinkedBlockingQueue<>(logBuffer);
        logBuffer.clear();
        logBuffer.addAll(retryQueue);

        // 若缓冲区已满则移除最早的日志
        while (logBuffer.remainingCapacity() == 0) {
            logBuffer.poll();
        }
    }

    // 日志清理任务
    private static final Runnable cleanupTask = new Runnable() {
        @Override
        public void run() {
            // 删除旧日志
            deleteOldLogs();
            // 重新安排清理任务
            scheduleCleanupTask();
        }
    };

    /**
     * 安排日志清理任务
     */
    private static void scheduleCleanupTask() {
        logHandler.postDelayed(cleanupTask, TimeUnit.DAYS.toMillis(1));
    }

    /**
     * 删除旧的日志文件
     */
    private static void deleteOldLogs() {
        if (logFilePath == null) return;

        File logDir = new File(logFilePath).getParentFile();
        if (logDir == null || !logDir.exists()) return;

        long cutoff = System.currentTimeMillis() - LOG_RETENTION_DAYS;
        File[] files = logDir.listFiles(file -> file.getName().matches(".*\\.(log|bak)$"));

        if (files == null) return;

        // 删除超过保留时长的日志文件
        for (File file : files) {
            if (file.lastModified() < cutoff && !file.delete()) {
                Log.w(TAG, "Failed to delete old log: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * 通知日志写入成功
     */
    private static void notifySuccess() {
        if (globalLogCallback == null) return;

        mainHandler.post(() -> {
            int count = successCount.getAndSet(0);
            if (count > 0) {
                globalLogCallback.onLogSuccess(count);
            }
        });
    }

    /**
     * 通知日志写入失败
     */
    private static void notifyFailure() {
        if (globalLogCallback == null) return;

        mainHandler.post(() -> {
            int count = failureCount.getAndSet(0);
            if (count > 0) {
                globalLogCallback.onLogFailure(count);
            }
        });
    }

    /**
     * 处理日志缓冲区已满的情况
     *
     * @param rejectedLog 被拒绝的日志消息
     */
    private static void handleBufferFull(String rejectedLog) {
        // 失败计数加1
        failureCount.incrementAndGet();

        if (globalLogCallback != null) {
            mainHandler.post(() -> globalLogCallback.onBufferFull(rejectedLog));
        }

        // 若不是错误日志则移除最早的日志
        if (!rejectedLog.contains(" E/")) {
            logBuffer.poll();
        }
    }

    /**
     * 获取当前日期
     *
     * @return 当前日期字符串
     */
    private static String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /**
     * 日志回调接口
     */
    public interface LogCallback {
        /**
         * 日志写入成功回调
         *
         * @param successCount 成功写入的日志数量
         */
        void onLogSuccess(int successCount);

        /**
         * 日志写入失败回调
         *
         * @param failureCount 失败写入的日志数量
         */
        void onLogFailure(int failureCount);

        /**
         * 日志缓冲区已满回调
         *
         * @param rejectedLog 被拒绝的日志消息
         */
        void onBufferFull(String rejectedLog);
    }
}