package com.wty.foundation.common.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 用于管理文件下载任务，支持断点续传、重试、并发控制、暂停和恢复下载等功能，
 * 可检查资源可达性和部分内容支持，实时反馈下载进度、速度、暂停和恢复状态。
 */
public class DownloadUtils {
    private static final String TAG = "DownloadUtils";
    private OkHttpClient okHttpClient;
    // 存储正在进行的下载任务，键为唯一标识，值为对应的 CallInfo 对象
    private ConcurrentHashMap<String, CallInfo> downloadTasks = new ConcurrentHashMap<>();
    // 信号量，控制最大并发下载数
    private final Semaphore downloadSemaphore = new Semaphore(5);
    // 最大重试次数
    private final int MAX_RETRIES = 3;

    /**
     * 默认构造函数，创建具有默认超时配置的 OkHttpClient。
     */
    public DownloadUtils() {
        this(new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build());
    }

    /**
     * 带自定义 OkHttpClient 的构造函数。
     *
     * @param client 自定义的 OkHttpClient 实例，若为 null 则使用默认实例。
     */
    public DownloadUtils(OkHttpClient client) {
        this.okHttpClient = client != null ? client : new OkHttpClient();
    }

    /**
     * 开始下载文件。先检查 URL 可达性和存储目录是否存在，满足条件则启动下载。
     *
     * @param url      文件下载链接
     * @param saveDir  存储下载文件的目录
     * @param fileName 文件名
     * @param listener 下载监听器，用于接收下载状态、进度和速度的回调
     */
    public void download(String url, String saveDir, String fileName, OnDownloadListener listener) {
        checkUrlReachable(url, reachable -> {
            if (!reachable) {
                notifyOnMainThread(() -> listener.onDownloadFailed("目标资源不可达"));
                return;
            }
            if (!ensureDirectoryExists(saveDir)) {
                notifyOnMainThread(() -> listener.onDownloadFailed("无法创建或访问存储目录"));
                return;
            }
            startDownload(url, saveDir, fileName, listener, 0);
        });
    }

    /**
     * 启动文件下载任务，考虑并发下载限制和部分内容支持。
     *
     * @param url        文件下载链接
     * @param saveDir    存储下载文件的目录
     * @param fileName   文件名
     * @param listener   下载监听器
     * @param retryCount 当前重试次数
     */
    private void startDownload(String url, String saveDir, String fileName, OnDownloadListener listener, int retryCount) {
        try {
            // 获取信号量许可
            downloadSemaphore.acquire();
            File file = new File(saveDir, fileName);
            long existingLength = file.exists() ? file.length() : 0;
            final long startFrom = existingLength;

            Request request = new Request.Builder().url(url).header("RANGE", "bytes=" + startFrom + "-").build();

            if (startFrom > 0 && !checkPartialContentSupport(url)) {
                file.delete();
                request = new Request.Builder().url(url).build(); // 不带 Range 头的请求
            }

            Call call = okHttpClient.newCall(request);
            String taskId = generateUniqueId(url, saveDir, fileName);

            downloadTasks.put(taskId, new CallInfo(call, startFrom)); // 使用唯一标识作为任务键
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handleFailure(retryCount, url, saveDir, fileName, listener, e, taskId);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        handleResponse(response, file, listener, startFrom, taskId);
                    } finally {
                        computeIfPresent(taskId, null); // 线程安全地移除下载任务
                        downloadSemaphore.release(); // 释放信号量
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            downloadSemaphore.release(); // 保证在中断时也释放信号量
            notifyOnMainThread(() -> listener.onDownloadFailed("任务被中断"));
        }
    }

    /**
     * 暂停指定的下载任务。
     *
     * @param taskId 下载任务的唯一标识
     */
    public void pauseDownload(String taskId) {
        downloadTasks.computeIfPresent(taskId, (key, value) -> {
            value.isPaused = true;
            if (value.call != null && !value.call.isCanceled()) {
                value.call.cancel();
            }
            return value;
        });
    }

    /**
     * 恢复指定的下载任务。
     *
     * @param url      文件下载链接
     * @param saveDir  存储下载文件的目录
     * @param fileName 文件名
     * @param listener 下载监听器
     */
    public void resumeDownload(String url, String saveDir, String fileName, OnDownloadListener listener) {
        String taskId = generateUniqueId(url, saveDir, fileName);
        downloadTasks.computeIfPresent(taskId, (key, value) -> {
            if (value != null && value.isPaused) {
                synchronized (value) {
                    value.isPaused = false;
                    if (value.isWaiting) {
                        value.notify(); // 只唤醒当前任务
                    }
                }
            } else {
                notifyOnMainThread(() -> listener.onDownloadFailed("任务未暂停或已不存在"));
            }
            return value;
        });
    }

    /**
     * 处理下载失败情况，决定是否重试。若重试次数未达上限，采用指数退避策略。
     *
     * @param retryCount 当前重试次数
     * @param url        文件下载链接
     * @param saveDir    存储下载文件的目录
     * @param fileName   文件名
     * @param listener   下载监听器
     * @param e          下载失败的异常信息
     * @param taskId     下载任务的唯一标识
     */
    private void handleFailure(int retryCount, String url, String saveDir, String fileName, OnDownloadListener listener, IOException e, String taskId) {
        computeIfPresent(taskId, null); // 移除无效任务
        downloadSemaphore.release(); // 释放信号量
        if (retryCount < MAX_RETRIES) {
            long delay = (long) Math.pow(2, retryCount) * 1000; // 指数退避
            if (delay > 30000) delay = 30000; // 设置最大重试间隔为 30 秒
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startDownload(url, saveDir, fileName, listener, retryCount + 1);
            }, delay);
        } else {
            cleanupTempFile(fileName, saveDir);
            notifyOnMainThread(() -> listener.onDownloadFailed("重试次数已达上限: " + e.getMessage()));
        }
    }

    /**
     * 处理下载响应，将文件写入本地并更新下载进度和速度，支持暂停和恢复。
     *
     * @param response  下载响应对象
     * @param file      存储下载文件的本地 File 对象
     * @param listener  下载监听器
     * @param startFrom 本地已存在文件的大小
     * @param taskId    下载任务的唯一标识
     * @throws IOException 若在文件读写过程中出现异常
     */
    private void handleResponse(Response response, File file, OnDownloadListener listener, long startFrom, String taskId) throws IOException {
        if (!response.isSuccessful()) {
            cleanupTempFile(file.getName(), file.getParent());
            notifyOnMainThread(() -> listener.onDownloadFailed("Unexpected code " + response.code()));
            return;
        }

        AtomicLong lastReportTime = new AtomicLong(System.currentTimeMillis());
        AtomicLong bytesSinceLastReport = new AtomicLong(0);
        try (InputStream inputStream = response.body().byteStream(); RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(startFrom); // 定位到文件末尾
            byte[] buffer = new byte[8192];
            long totalBytes = response.body().contentLength() + file.length();
            long downloadedBytes = startFrom;

            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                synchronized (downloadTasks.get(taskId)) { // 添加同步块确保线程安全
                    if (downloadTasks.get(taskId).isPaused) {
                        downloadTasks.get(taskId).isWaiting = true;
                        notifyOnMainThread(listener::onDownloadPaused);
                        try {
                            downloadTasks.get(taskId).wait(); // 等待直到任务被恢复
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            notifyOnMainThread(() -> listener.onDownloadFailed("任务被中断"));
                            return;
                        }
                        downloadTasks.get(taskId).isWaiting = false;
                        notifyOnMainThread(listener::onDownloadResumed);
                    }
                }
                raf.write(buffer, 0, readBytes);
                downloadedBytes += readBytes;
                bytesSinceLastReport.addAndGet(readBytes);
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - lastReportTime.get();
                if (elapsedTime > 0) {
                    double speed = bytesSinceLastReport.get() / (elapsedTime / 1000.0); // 改为 double 类型
                    notifyOnMainThread(() -> listener.onSpeedUpdate(speed)); // 确保监听器也接受 double 类型参数
                    lastReportTime.set(currentTime);
                    bytesSinceLastReport.set(0);
                }
                int progress = (int) ((downloadedBytes * 100) / totalBytes);
                notifyOnMainThread(() -> listener.onDownloading(progress));
            }
            notifyOnMainThread(listener::onDownloadSuccess);
        } catch (SecurityException e) {
            notifyOnMainThread(() -> listener.onDownloadFailed("权限错误: " + e.getMessage()));
        } catch (IOException e) {
            notifyOnMainThread(() -> listener.onDownloadFailed("网络错误: " + e.getMessage()));
        } catch (Exception e) {
            notifyOnMainThread(() -> listener.onDownloadFailed("未知错误: " + e.getMessage()));
        }
    }

    /**
     * 确保存储下载文件的目录存在，若不存在则尝试创建。
     *
     * @param saveDir 存储下载文件的目录路径
     * @return 若目录存在或成功创建则返回 true，否则返回 false
     */
    private boolean ensureDirectoryExists(String saveDir) {
        File dir = new File(saveDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        return true;
    }

    /**
     * 清理可能产生的临时文件。
     *
     * @param fileName 要清理的文件名称
     * @param saveDir  存储文件的目录路径
     */
    private void cleanupTempFile(String fileName, String saveDir) {
        File dir = new File(saveDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("保存目录无效");
        }
        File file = new File(dir, fileName);
        if (file.exists()) {
            file.delete(); // 删除整个文件
        }
    }

    /**
     * 检查给定 URL 是否可达，并检查是否支持断点续传。
     *
     * @param url      要检查的 URL
     * @param callback 用于接收检查结果的回调函数
     */
    private void checkUrlReachable(String url, Consumer<Boolean> callback) {
        Request request = new Request.Builder().url(url).head().addHeader("Range", "bytes=0-").build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.accept(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.accept(response.isSuccessful() && response.code() == 206);
            }
        });
    }

    /**
     * 检查服务器是否支持部分内容下载。
     *
     * @param url 要检查的 URL
     * @return 若支持则返回 true，否则返回 false
     */
    private boolean checkPartialContentSupport(String url) {
        Request request = new Request.Builder().url(url).head().addHeader("Range", "bytes=0-").build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            return response.code() == 206; // 206 Partial Content
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 生成下载任务的唯一标识。
     *
     * @param url      下载链接
     * @param saveDir  存储目录
     * @param fileName 文件名
     * @return 唯一标识的字符串
     */
    private String generateUniqueId(String url, String saveDir, String fileName) {
        url = url.trim();
        saveDir = saveDir.trim();
        fileName = fileName.trim();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String baseString = url + saveDir + fileName;
            byte[] hash = md.digest(baseString.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 若任务存在，则根据传入的 Call 对象更新或移除任务。
     *
     * @param taskId 下载任务的唯一标识
     * @param call   下载任务的 Call 对象，若为 null 则移除任务
     */
    private void computeIfPresent(String taskId, Call call) {
        downloadTasks.computeIfPresent(taskId, (key, value) -> {
            if (call == null || value.call.isCanceled() || value.call.isExecuted()) {
                return null; // Remove the entry from the map
            }
            return value;
        });
    }

    /**
     * 在主线程执行指定的操作。
     *
     * @param action 要执行的操作
     */
    private void notifyOnMainThread(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            new Handler(Looper.getMainLooper()).post(action);
        }
    }

    /**
     * 在主线程执行指定的消费操作。
     *
     * @param action 要执行的消费操作
     * @param param  消费操作的参数
     * @param <T>    参数的类型
     */
    private <T> void notifyOnMainThread(Consumer<T> action, T param) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.accept(param);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> action.accept(param));
        }
    }

    /**
     * 下载监听器接口，用于监听文件下载过程中的各种状态变化。
     */
    public interface OnDownloadListener {
        /**
         * 当文件下载成功时调用此方法。
         */
        void onDownloadSuccess();

        /**
         * 当文件正在下载时，定期调用此方法反馈下载进度。
         *
         * @param progress 当前的下载进度，取值范围为 0 - 100
         */
        void onDownloading(int progress);

        /**
         * 当文件下载失败时调用此方法，并提供失败的错误信息。
         *
         * @param errorMessage 描述下载失败原因的错误信息
         */
        void onDownloadFailed(String errorMessage);

        /**
         * 当下载速度发生变化时调用此方法，反馈当前的下载速度。
         *
         * @param speed 当前的下载速度，单位为字节每秒
         */
        void onSpeedUpdate(double speed);

        /**
         * 当下载任务被暂停时调用此方法。
         */
        void onDownloadPaused();

        /**
         * 当下载任务从暂停状态恢复时调用此方法。
         */
        void onDownloadResumed();
    }

    /**
     * 内部静态类，用于存储下载任务的相关信息。
     */
    private static class CallInfo {
        // OkHttp 的 Call 对象，代表一个可执行的请求
        Call call;
        // 下载开始的位置，用于支持断点续传
        long startPosition;
        // 标记下载任务是否处于暂停状态
        boolean isPaused;
        // 标记下载任务是否正在等待恢复
        boolean isWaiting;

        /**
         * 构造函数，初始化下载任务的相关信息。
         *
         * @param call          OkHttp 的 Call 对象，不能为 null
         * @param startPosition 下载开始的位置
         */
        CallInfo(@NonNull Call call, long startPosition) {
            this.call = call;
            this.startPosition = startPosition;
            // 初始状态为未暂停
            this.isPaused = false;
            // 初始状态为未等待恢复
            this.isWaiting = false;
        }
    }
}