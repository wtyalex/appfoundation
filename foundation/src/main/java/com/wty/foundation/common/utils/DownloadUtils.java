package com.wty.foundation.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.RequiresApi;

import com.wty.foundation.common.init.AppContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 下载管理器，支持多任务、断点续传、智能重试和完整性校验
 */
public class DownloadUtils {
    private static final String TAG = "DownloadUtils";
    // 下载超时时间，单位毫秒
    private static final long DOWNLOAD_TIMEOUT_MS = 60000;
    // 最大并发下载任务数
    private static final int MAX_CONCURRENT_DOWNLOADS = 5;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 缓冲区大小
    private static final int BUFFER_SIZE = 8192;
    // 最小进度更新间隔，单位毫秒
    private static final long MIN_PROGRESS_UPDATE_INTERVAL = 300;

    private final OkHttpClient client;
    // 存储活跃的下载任务上下文
    private final ConcurrentHashMap<String, TaskContext> activeTasks = new ConcurrentHashMap<>();
    // 控制并发下载任务数的信号量
    private final Semaphore concurrencySemaphore = new Semaphore(MAX_CONCURRENT_DOWNLOADS, true);
    private static volatile DownloadUtils instance;
    // 用于在主线程处理任务的Handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 用于超时监控的Handler
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    /**
     * 获取下载工具单例实例
     *
     * @return DownloadUtils实例
     */
    public static DownloadUtils getInstance() {
        if (instance == null) {
            synchronized (DownloadUtils.class) {
                if (instance == null) {
                    instance = new DownloadUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 私有构造方法，用于初始化OkHttpClient并启动超时监测
     */
    private DownloadUtils() {
        this.client = createSecureClient();
        startTimeoutMonitor();
    }

    /**
     * 创建安全的OkHttpClient实例
     *
     * @return OkHttpClient实例
     */
    private OkHttpClient createSecureClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT)).build();
    }

    /**
     * 启动下载任务
     *
     * @param url      下载文件的URL
     * @param savePath 文件保存路径
     * @param fileName 文件名
     * @param callback 下载回调接口
     * @return 任务ID，如果输入参数无效则返回 ""
     */
    public String startDownload(String url, String savePath, String fileName, DownloadCallback callback) {
        if (!validateInputs(url, savePath, fileName, callback)) {
            return "";
        }

        final String taskId = generateTaskId(url, savePath, fileName);
        if (activeTasks.containsKey(taskId)) {
            notifyErrorImmediately(callback, taskId, "任务已存在");
            return taskId;
        }

        new Thread(() -> {
            try {
                concurrencySemaphore.acquire();
                final File targetFile = prepareFile(savePath, fileName, callback);
                if (targetFile == null) return;

                final TaskContext context = new TaskContext(taskId, url, savePath, fileName, callback);
                activeTasks.put(taskId, context);

                boolean supportsResume = checkResumeSupport(url);
                executeDownload(context, targetFile, 0, supportsResume);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                concurrencySemaphore.release();
            }
        }).start();

        return taskId;
    }

    /**
     * 检查URL是否支持断点续传
     *
     * @param url 下载文件的URL
     * @return 如果支持断点续传则返回true，否则返回false
     */
    private boolean checkResumeSupport(String url) {
        Request request = new Request.Builder().url(url).head().build();

        try (Response response = client.newCall(request).execute()) {
            return response.code() == 206; // 检查是否支持分片下载
        } catch (IOException e) {
            Log.w(TAG, "断点续传检查失败", e);
            return false;
        }
    }

    /**
     * 执行下载
     *
     * @param context    任务上下文
     * @param targetFile 目标文件
     * @param retryCount 重试次数
     * @param useRange   是否使用范围请求（断点续传）
     */
    private void executeDownload(TaskContext context, File targetFile, int retryCount, boolean useRange) {
        if (context.isCancelled.get()) {
            cleanupTask(context.taskId, "任务已取消");
            return;
        }

        final long startBytes = useRange ? targetFile.length() : 0;
        Request request = new Request.Builder().url(context.url).header("Range", "bytes=" + startBytes + "-").build();

        context.currentCall.set(client.newCall(request));
        context.currentCall.get().enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleNetworkError(context, targetFile, retryCount, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body(); InputStream is = body.byteStream(); RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {

                    if (!response.isSuccessful()) {
                        handleServerError(context, response.code());
                        return;
                    }

                    // 处理文件长度信息
                    long contentLength = body.contentLength();
                    long totalSize = contentLength + (useRange ? targetFile.length() : 0);
                    context.totalSize.set(totalSize);

                    raf.seek(startBytes);
                    processStream(context, is, raf, totalSize);
                    verifyAndFinalize(context, targetFile);
                } catch (Exception e) {
                    handleUnexpectedError(context, e);
                }
            }
        });
    }

    /**
     * 处理下载数据流
     *
     * @param context   任务上下文
     * @param is        输入流
     * @param raf       随机访问文件
     * @param totalSize 文件总大小
     * @throws IOException 输入输出异常
     */
    private void processStream(TaskContext context, InputStream is, RandomAccessFile raf, long totalSize) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long lastSpeedUpdateTime = System.currentTimeMillis(); // 速度计算起始时间
        long bytesInCurrentWindow = 0; // 当前统计窗口的字节数

        try {
            while ((bytesRead = is.read(buffer)) != -1) {
                // 检查暂停状态（同步锁保证线程安全）
                checkPauseState(context);

                // 检查取消状态
                if (context.isCancelled.get()) {
                    Log.d(TAG, "任务已取消: " + context.taskId);
                    break;
                }

                // 写入文件（处理潜在IO异常）
                try {
                    raf.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    Log.e(TAG, "文件写入失败: " + e.getMessage());
                }

                // 更新进度（整合updateProgress逻辑）
                updateProgress(context, bytesRead, totalSize, lastSpeedUpdateTime, bytesInCurrentWindow);

                // 记录最后有效进度时间（用于超时监控）
                context.lastProgressTime.set(System.currentTimeMillis());
            }

            // 最终完成检测（确保完成回调触发）
            if (context.downloadedBytes.get() >= totalSize && !context.isCancelled.get()) {
                context.isCompleted.set(true);
                Log.i(TAG, "下载完成: " + context.taskId);
            }
        } finally {
            // 强制刷新文件系统缓存
            try {
                raf.getFD().sync();
            } catch (SyncFailedException e) {
                Log.w(TAG, "文件同步失败: " + e.getMessage());
            }
        }
    }

    /**
     * 进度更新
     *
     * @param context              任务上下文
     * @param delta                本次读取字节数
     * @param totalSize            总文件大小
     * @param lastSpeedUpdateTime  上次速度计算时间（传引用）
     * @param bytesInCurrentWindow 当前统计窗口字节数（传引用）
     */
    private void updateProgress(TaskContext context, int delta, long totalSize, long lastSpeedUpdateTime, long bytesInCurrentWindow) {
        // 更新累计下载量
        long currentBytes = context.downloadedBytes.addAndGet(delta);

        // 统计当前窗口数据
        bytesInCurrentWindow += delta;

        // 计算时间差
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastSpeedUpdateTime;

        // 满足以下条件之一时触发回调：
        // 1. 超过最小更新间隔（300ms）
        // 2. 下载完成
        // 3. 统计窗口超过1MB（避免小包频繁回调）
        if (timeDelta > MIN_PROGRESS_UPDATE_INTERVAL || currentBytes == totalSize || bytesInCurrentWindow > 1048576) {

            double speed = calculateSpeed(bytesInCurrentWindow, timeDelta);
            notifyProgress(context, currentBytes, totalSize, speed);

            // 重置统计窗口
            lastSpeedUpdateTime = currentTime;
            bytesInCurrentWindow = 0;
        }
    }

    /**
     * 校验文件并完成下载
     *
     * @param context 任务上下文
     * @param tmpFile 临时文件
     */
    private void verifyAndFinalize(TaskContext context, File tmpFile) {
        if (context.isCancelled.get()) {
            safeDeleteFile(tmpFile);
            return;
        }

        // 完整性校验
        if (tmpFile.length() != context.totalSize.get()) {
            handleFileFinalizeError(context, tmpFile);
            return;
        }

        File finalFile = new File(context.savePath, context.fileName);
        if (tmpFile.renameTo(finalFile)) {
            Log.d(TAG, "文件重命名成功：" + finalFile.getAbsolutePath());
            notifyCompletion(context, finalFile);
        } else {
            Log.e(TAG, "文件重命名失败！源文件：" + tmpFile.length() + "字节，目标：" + finalFile.getAbsolutePath());
            handleFileFinalizeError(context, tmpFile);
        }
        cleanupTask(context.taskId, "下载完成");
    }

    /**
     * 处理文件最终化错误
     *
     * @param context 任务上下文
     * @param tmpFile 临时文件
     */
    private void handleFileFinalizeError(TaskContext context, File tmpFile) {
        Log.e(TAG, "文件重命名失败：" + tmpFile.getAbsolutePath());
        notifyError(context, "文件保存失败");
        safeDeleteFile(tmpFile);
        cleanupTask(context.taskId, "文件错误");
    }

    /**
     * 暂停检查逻辑
     *
     * @param context 任务上下文
     */
    private void checkPauseState(TaskContext context) {
        synchronized (context.pauseLock) {
            while (context.isPaused.get() && !context.isCancelled.get()) {
                try {
                    notifyPaused(context);
                    context.pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 处理网络错误（带指数退避重试）
     *
     * @param context    任务上下文
     * @param tmpFile    临时文件
     * @param retryCount 重试次数
     * @param e          异常对象
     */
    private void handleNetworkError(TaskContext context, File tmpFile, int retryCount, Exception e) {
        if (retryCount < MAX_RETRIES && !context.isCancelled.get()) {
            long delay = (long) (1000 * Math.pow(2, retryCount));
            mainHandler.postDelayed(() -> executeDownload(context, tmpFile, retryCount + 1, true), delay);
        } else {
            notifyError(context, "网络错误: " + e.getMessage());
            safeDeleteFile(tmpFile);
            cleanupTask(context.taskId, "网络错误");
        }
    }

    /**
     * 暂停下载任务
     *
     * @param taskId 任务ID
     */
    public void pauseDownload(String taskId) {
        TaskContext context = activeTasks.get(taskId);
        if (context != null) {
            context.isPaused.set(true);
            Call call = context.currentCall.get();
            if (call != null) call.cancel();
        }
    }

    /**
     * 恢复下载任务
     *
     * @param taskId 任务ID
     */
    public void resumeDownload(String taskId) {
        TaskContext context = activeTasks.get(taskId);
        if (context != null) {
            synchronized (context.pauseLock) {
                context.isPaused.set(false);
                context.pauseLock.notifyAll();
            }
            if (!isNetworkAvailable()) {
                notifyError(context, "网络不可用");
                return;
            }
            File tmpFile = new File(context.savePath, context.fileName + ".tmp");
            executeDownload(context, tmpFile, 0, true);
        }
    }

    /**
     * 取消下载任务
     *
     * @param taskId 任务ID
     */
    public void cancelDownload(String taskId) {
        TaskContext context = activeTasks.get(taskId);
        if (context != null) {
            context.isCancelled.set(true);
            Call call = context.currentCall.get();
            if (call != null) call.cancel();
            safeDeleteFile(new File(context.savePath, context.fileName + ".tmp"));
            cleanupTask(taskId, "用户取消");
        }
    }

    /**
     * 超时监控
     */
    private void startTimeoutMonitor() {
        timeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    handleTimeoutWithRemoveIf(now);
                } else {
                    handleTimeoutLegacy(now);
                }
                timeoutHandler.postDelayed(this, 30000);
            }

            @RequiresApi(api = android.os.Build.VERSION_CODES.N)
            private void handleTimeoutWithRemoveIf(long now) {
                activeTasks.entrySet().removeIf(entry -> {
                    TaskContext ctx = entry.getValue();
                    return checkAndHandleTimeout(ctx, now);
                });
            }

            private void handleTimeoutLegacy(long now) {
                Iterator<Map.Entry<String, TaskContext>> iterator = activeTasks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, TaskContext> entry = iterator.next();
                    if (checkAndHandleTimeout(entry.getValue(), now)) {
                        iterator.remove();
                    }
                }
            }

            private boolean checkAndHandleTimeout(TaskContext ctx, long now) {
                boolean timeout = now - ctx.lastProgressTime.get() > DOWNLOAD_TIMEOUT_MS;
                if (timeout && !ctx.isCompleted.get()) {
                    handleStagnation(ctx);
                    return true;
                }
                return false;
            }
        }, 30000);
    }

    /**
     * 处理下载停滞（超时）情况
     *
     * @param context 任务上下文
     */
    private void handleStagnation(TaskContext context) {
        notifyError(context, "下载超时");
        safeDeleteFile(new File(context.savePath, context.fileName + ".tmp"));
        context.currentCall.get().cancel();
        cleanupTask(context.taskId, "超时清理");
    }

    /**
     * 验证输入参数有效性
     *
     * @param url      下载的URL
     * @param path     保存文件的路径
     * @param name     文件名
     * @param callback 下载回调
     * @return 输入参数是否有效
     */
    private boolean validateInputs(String url, String path, String name, DownloadCallback callback) {
        if (TextUtils.isEmpty(url)) {
            notifyErrorImmediately(callback, null, "URL不能为空");
            return false;
        }

        // URL格式校验
        if (!isValidUrl(url)) {
            notifyErrorImmediately(callback, null, "URL格式无效");
            return false;
        }

        // 检查路径或文件名是否为空
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(name)) {
            notifyErrorImmediately(callback, null, "无效路径或文件名");
            return false;
        }

        // 检查目录是否存在，不存在则创建
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            notifyErrorImmediately(callback, null, "目录创建失败");
            return false;
        }

        return true;
    }

    /**
     * 验证传入的URL是否有效
     *
     * @param url 待验证的URL字符串
     * @return 如果URL有效则返回true，否则返回false
     */
    private boolean isValidUrl(String url) {
        // 允许的协议白名单
        final String[] ALLOWED_SCHEMES = {"http", "https", "ftp"};

        // 使用Android系统URL检测模式
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return false;
        }

        // 验证协议类型
        try {
            String scheme = Uri.parse(url).getScheme().toLowerCase();
            return Arrays.asList(ALLOWED_SCHEMES).contains(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 立即通知错误
     *
     * @param callback 下载回调
     * @param taskId   任务ID
     * @param message  错误消息
     */
    private void notifyErrorImmediately(DownloadCallback callback, String taskId, String message) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onError(taskId, message));
    }

    /**
     * 准备下载文件（带旧文件清理）
     *
     * @param path     保存文件的路径
     * @param name     文件名
     * @param callback 下载回调
     * @return 临时文件对象，若准备失败则返回null
     */
    private File prepareFile(String path, String name, DownloadCallback callback) {
        File tmpFile = new File(path, name + ".tmp");
        File finalFile = new File(path, name);

        try {
            // 清理已有文件
            if (finalFile.exists() && !finalFile.delete()) {
                notifyErrorImmediately(callback, null, "已有文件无法删除");
                return null;
            }
            // 清理临时文件
            if (tmpFile.exists() && !tmpFile.delete()) {
                notifyErrorImmediately(callback, null, "临时文件无法清理");
                return null;
            }
            // 创建临时文件
            if (!tmpFile.createNewFile()) {
                notifyErrorImmediately(callback, null, "文件创建失败");
                return null;
            }
        } catch (IOException e) {
            notifyErrorImmediately(callback, null, "IO错误: " + e.getMessage());
            return null;
        }
        return tmpFile;
    }

    /**
     * 生成唯一任务ID（带SHA - 256哈希）
     *
     * @param params 用于生成ID的参数
     * @return 生成的任务ID
     */
    private String generateTaskId(String... params) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(String.join("#", params).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16); // 取前16位作为ID
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 安全执行回调（主线程）
     *
     * @param context 任务上下文
     * @param action  要执行的操作
     */
    private void safeCallback(TaskContext context, Runnable action) {
        if (context.callbackRef.get() == null) return;
        mainHandler.post(() -> {
            if (!context.isCancelled.get()) {
                action.run();
            }
        });
    }

    /**
     * 计算下载速度（KB/s）
     *
     * @param bytesDelta  下载的字节数
     * @param timeDeltaMs 下载时间（毫秒）
     * @return 下载速度（KB/s）
     */
    private double calculateSpeed(long bytesDelta, long timeDeltaMs) {
        if (timeDeltaMs == 0) return 0;
        return (bytesDelta / 1024.0) / (timeDeltaMs / 1000.0);
    }

    /**
     * 网络可用性检查
     *
     * @return 网络是否可用
     */
    private boolean isNetworkAvailable() {
        Context ctx = AppContext.getInstance().getContext();
        if (ctx == null) return false;

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * 安全删除文件（静默处理异常）
     *
     * @param file 要删除的文件
     */
    private void safeDeleteFile(File file) {
        if (file == null) return;
        try {
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "文件删除失败: " + file.getAbsolutePath());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "文件删除权限不足: " + e.getMessage());
        }
    }

    /**
     * 处理服务器响应错误
     *
     * @param context    任务上下文
     * @param statusCode HTTP状态码
     */
    private void handleServerError(TaskContext context, int statusCode) {
        String errorMsg;
        switch (statusCode) {
            case 404:
                errorMsg = "资源不存在";
                break;
            case 416:
                errorMsg = "范围请求不满足";
                break;
            case 500:
            case 503:
                errorMsg = "服务器内部错误";
                break;
            default:
                errorMsg = "HTTP错误码: " + statusCode;
        }
        notifyError(context, errorMsg);
        cleanupTask(context.taskId, errorMsg);
    }

    /**
     * 处理未知错误
     *
     * @param context 任务上下文
     * @param e       异常对象
     */
    private void handleUnexpectedError(TaskContext context, Exception e) {
        Log.e(TAG, "未预期错误: " + e.getMessage(), e);
        notifyError(context, "系统错误: " + e.getClass().getSimpleName());
        cleanupTask(context.taskId, "未预期异常");
    }

    /**
     * 进度通知（带频率控制）
     *
     * @param context 任务上下文
     * @param current 当前下载的字节数
     * @param total   文件总字节数
     * @param speed   下载速度（KB/s）
     */
    private void notifyProgress(TaskContext context, long current, long total, double speed) {
        safeCallback(context, () -> {
            DownloadCallback cb = context.callbackRef.get();
            if (cb != null) {
                cb.onProgress(context.taskId, current, total, speed);
            }
        });
    }

    /**
     * 完成通知
     *
     * @param context 任务上下文
     * @param file    下载完成的文件
     */
    private void notifyCompletion(TaskContext context, File file) {
        safeCallback(context, () -> {
            DownloadCallback cb = context.callbackRef.get();
            if (cb != null) {
                cb.onComplete(context.taskId, file);
            }
        });
    }

    /**
     * 错误通知
     *
     * @param context 任务上下文
     * @param reason  错误原因
     */
    private void notifyError(TaskContext context, String reason) {
        safeCallback(context, () -> {
            DownloadCallback cb = context.callbackRef.get();
            if (cb != null) {
                cb.onError(context.taskId, reason);
            }
        });
    }

    /**
     * 暂停通知
     *
     * @param context 任务上下文
     */
    private void notifyPaused(TaskContext context) {
        safeCallback(context, () -> {
            DownloadCallback cb = context.callbackRef.get();
            if (cb != null) {
                cb.onPaused(context.taskId, context.downloadedBytes.get());
            }
        });
    }

    /**
     * 清理任务资源
     *
     * @param taskId 任务ID
     * @param reason 清理原因
     */
    private void cleanupTask(String taskId, String reason) {
        TaskContext context = activeTasks.remove(taskId);
        if (context == null) return;

        Log.d(TAG, "清理任务: " + taskId + " 原因: " + reason);

        // 取消网络请求
        Call call = context.currentCall.get();
        if (call != null) call.cancel();

        // 清理临时文件（错误时）
        if (reason != null && !"下载完成".equals(reason)) {
            safeDeleteFile(new File(context.savePath, context.fileName + ".tmp"));
        }
    }

    /**
     * 任务上下文，存储单个下载任务的相关信息与状态
     */
    private static class TaskContext {
        // 任务唯一标识符，用于区分不同的下载任务
        final String taskId;
        // 下载文件的URL，指定了文件的来源地址
        final String url;
        // 文件保存路径，指明下载完成后文件存储的位置
        final String savePath;
        // 文件名，定义了下载文件在本地存储的名称
        final String fileName;
        //下载回调接口的软引用，在内存不足时可被回收以释放内存，用于通知下载状态
        final Reference<DownloadCallback> callbackRef;
        // 已下载字节数，记录当前任务已完成的下载量
        final AtomicLong downloadedBytes = new AtomicLong();
        // 文件总字节数，代表整个文件的大小
        final AtomicLong totalSize = new AtomicLong();
        // 任务是否暂停的标志，控制任务的暂停与恢复
        final AtomicBoolean isPaused = new AtomicBoolean();
        // 任务是否取消的标志，用于取消正在进行的下载任务
        final AtomicBoolean isCancelled = new AtomicBoolean();
        // 任务是否完成的标志，判断下载任务是否成功结束
        final AtomicBoolean isCompleted = new AtomicBoolean();
        // 当前网络请求调用，可对正在进行的网络请求进行操作
        final AtomicReference<Call> currentCall = new AtomicReference<>();
        // 用于暂停操作的锁对象，在任务暂停和恢复时实现同步
        final Object pauseLock = new Object();
        // 最后一次进度更新时间，用于超时监控
        final AtomicLong lastProgressTime = new AtomicLong(System.currentTimeMillis());

        /**
         * 构造任务上下文
         *
         * @param taskId   任务唯一标识符
         * @param url      下载文件的URL
         * @param path     文件保存路径
         * @param name     文件名
         * @param callback 下载回调接口
         */
        TaskContext(String taskId, String url, String path, String name, DownloadCallback callback) {
            this.taskId = taskId;
            this.url = url;
            this.savePath = path;
            this.fileName = name;
            this.callbackRef = new SoftReference<>(callback);
        }
    }

    public interface DownloadCallback {
        /**
         * 下载进度回调
         *
         * @param taskId     任务ID
         * @param downloaded 已下载的字节数
         * @param total      文件总字节数
         * @param speed      下载速度（KB/s）
         */
        void onProgress(String taskId, long downloaded, long total, double speed);

        /**
         * 下载完成回调
         *
         * @param taskId 任务ID
         * @param file   下载完成的文件
         */
        void onComplete(String taskId, File file);

        /**
         * 下载暂停回调
         *
         * @param taskId   任务ID
         * @param progress 暂停时的下载进度（字节数）
         */
        void onPaused(String taskId, long progress);

        /**
         * 下载错误回调
         *
         * @param taskId 任务ID
         * @param reason 错误原因
         */
        void onError(String taskId, String reason);
    }
}