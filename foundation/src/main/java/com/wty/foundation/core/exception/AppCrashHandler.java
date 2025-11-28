package com.wty.foundation.core.exception;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.wty.foundation.common.init.ActivityLifecycleManager;
import com.wty.foundation.common.utils.SPUtils;
import com.wty.foundation.core.base.activity.CrashDisplayActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author wutianyu
 * @createTime 2025/3/28
 * @describe 应用崩溃处理器，负责捕获异常、生成报告和保存日志
 */
public class AppCrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "AppCrashHandler";
    private static final int DEFAULT_MAX_LOG_FILES = 20;
    private static final String CRASH_LOG_SUBDIR = "CrashLogs";
    private static final String SP_KEY_LAST_CRASH_TIME = "app_crash_last_time";
    private static AppCrashHandler instance;

    private final Context mContext;
    private final Thread.UncaughtExceptionHandler mDefaultHandler;
    private final String mAppName;
    private final SPUtils mSpUtils;
    private final ActivityLifecycleManager mActivityLifecycleManager;

    // 配置参数
    private int mMaxLogFiles = DEFAULT_MAX_LOG_FILES;
    private boolean mEnabled = true;
    private boolean mShowRestart = true;
    private boolean mShowCopy = true;
    private boolean mShowCrashInfo = false;
    private boolean mShowCrashActivity = true;
    private boolean mSaveLogFile = false;
    private long mCrashInterval = 5000;
    private boolean mTrackActivities = true;
    private File mCustomLogSavePath;
    private Class<? extends Activity> mRestartActivity;
    private Class<? extends Activity> mCrashActivity = CrashDisplayActivity.class;
    private final List<CrashListener> mListeners = new ArrayList<>();

    /**
     * 崩溃监听接口
     */
    public interface CrashListener {
        void onCrashOccurred(Thread thread, Throwable ex, String crashLog);

        default void beforeRestart() {
        }

        default void beforeExit() {
        }
    }

    /**
     * 私有构造函数
     *
     * @param context 上下文
     */
    private AppCrashHandler(Context context) {
        this.mContext = context.getApplicationContext();
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.mAppName = getAppName(context);
        this.mSpUtils = SPUtils.getInstance();
        this.mActivityLifecycleManager = ActivityLifecycleManager.getInstance();
    }

    /**
     * 获取单例实例
     *
     * @return AppCrashHandler实例
     */
    public static AppCrashHandler getInstance() {
        return instance;
    }

    /**
     * 初始化崩溃处理器
     *
     * @param context 上下文
     * @return 配置器
     */
    public static synchronized Configurator initCrashHandler(Context context) {
        if (instance == null) {
            instance = new AppCrashHandler(context);
            Thread.setDefaultUncaughtExceptionHandler(instance);
        }
        return new Configurator(instance);
    }

    /**
     * 配置器（链式调用）
     */
    public static class Configurator {
        private final AppCrashHandler mHandler;

        private Configurator(AppCrashHandler handler) {
            this.mHandler = handler;
        }

        /**
         * 设置最大日志文件数量
         *
         * @param max 最大数量（最小为1）
         * @return 配置器
         */
        public Configurator setMaxLogFiles(int max) {
            mHandler.mMaxLogFiles = Math.max(1, max);
            return this;
        }

        /**
         * 设置重启后跳转的Activity
         *
         * @param activity 目标Activity
         * @return 配置器
         */
        public Configurator setRestartActivity(Class<? extends Activity> activity) {
            mHandler.mRestartActivity = activity;
            return this;
        }

        /**
         * 设置崩溃展示Activity
         *
         * @param activity 自定义崩溃页面
         * @return 配置器
         */
        public Configurator setCrashActivity(Class<? extends Activity> activity) {
            mHandler.mCrashActivity = activity;
            return this;
        }

        /**
         * 设置崩溃页面按钮显示状态
         *
         * @param showRestart 是否显示重启按钮
         * @param showCopy    是否显示复制日志按钮
         * @return 配置器
         */
        public Configurator setShowButtons(boolean showRestart, boolean showCopy) {
            mHandler.mShowRestart = showRestart;
            mHandler.mShowCopy = showCopy;
            return this;
        }

        /**
         * 启用/禁用崩溃处理
         *
         * @param enabled 是否启用
         * @return 配置器
         */
        public Configurator setEnabled(boolean enabled) {
            mHandler.mEnabled = enabled;
            return this;
        }

        /**
         * 设置是否显示崩溃详情
         *
         * @param show 是否显示
         * @return 配置器
         */
        public Configurator setShowCrashInfo(boolean show) {
            mHandler.mShowCrashInfo = show;
            return this;
        }

        /**
         * 设置是否显示崩溃页面
         *
         * @param show 是否显示
         * @return 配置器
         */
        public Configurator setShowCrashActivity(boolean show) {
            mHandler.mShowCrashActivity = show;
            return this;
        }

        /**
         * 设置是否保存崩溃日志到文件
         *
         * @param save 是否保存
         * @return 配置器
         */
        public Configurator setSaveLogFile(boolean save) {
            mHandler.mSaveLogFile = save;
            return this;
        }

        /**
         * 设置连续崩溃保护间隔（毫秒）
         *
         * @param interval 间隔时间（最小1000ms）
         * @return 配置器
         */
        public Configurator setCrashInterval(long interval) {
            mHandler.mCrashInterval = Math.max(1000, interval);
            return this;
        }

        /**
         * 设置是否跟踪Activity生命周期
         *
         * @param track 是否跟踪
         * @return 配置器
         */
        public Configurator setTrackActivities(boolean track) {
            mHandler.mTrackActivities = track;
            return this;
        }

        /**
         * 设置自定义日志保存目录
         *
         * @param path 期望保存的目录（如 Download）
         * @return 配置器
         */
        public Configurator setLogSavePath(File path) {
            mHandler.mCustomLogSavePath = path;
            return this;
        }

        /**
         * 添加崩溃监听器
         *
         * @param listener 监听器实例
         */
        public void addCrashListener(CrashListener listener) {
            if (!mHandler.mListeners.contains(listener)) {
                mHandler.mListeners.add(listener);
            }
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!mEnabled) {
            if (mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(thread, ex);
            }
            return;
        }

        if (Looper.getMainLooper().getThread() == thread) {
            handleMainThreadCrash(thread, ex);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> handleMainThreadCrash(thread, ex));
        }
    }

    /**
     * 处理主线程崩溃逻辑
     *
     * @param thread 崩溃线程
     * @param ex     异常信息
     */
    private void handleMainThreadCrash(Thread thread, Throwable ex) {
        if (!checkCrashInterval()) {
            exitProcess();
            return;
        }

        try {
            final String crashLog = buildCrashReport(ex);
            notifyCrashOccurred(thread, ex, crashLog);

            if (mShowCrashActivity) {
                launchCrashActivity(crashLog);
                new Thread(() -> {
                    saveCrashLog(crashLog);
                    new Handler(Looper.getMainLooper()).postDelayed(Looper::loop, 100);
                }).start();
                Looper.loop();
            } else {
                new Thread(() -> {
                    saveCrashLog(crashLog);
                    SystemClock.sleep(300);
                    exitProcess();
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle crash", e);
            exitProcess();
        }
    }

    /**
     * 检查连续崩溃间隔（避免无限崩溃循环）
     *
     * @return true：正常崩溃，false：连续崩溃需直接退出
     */
    private boolean checkCrashInterval() {
        long lastCrashTime = mSpUtils.getLong(SP_KEY_LAST_CRASH_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long interval = currentTime - lastCrashTime;

        if (lastCrashTime == 0 || interval >= mCrashInterval) {
            mSpUtils.putLong(SP_KEY_LAST_CRASH_TIME, currentTime, false);
            return true;
        } else {
            Log.w(TAG, "Continuous crash detected (interval: " + interval + "ms < " + mCrashInterval + "ms), exit directly");
            return false;
        }
    }

    /**
     * 通知所有崩溃监听器
     *
     * @param thread 崩溃线程
     * @param ex     异常信息
     * @param log    崩溃日志
     */
    private void notifyCrashOccurred(Thread thread, Throwable ex, String log) {
        for (CrashListener listener : mListeners) {
            try {
                listener.onCrashOccurred(thread, ex, log);
            } catch (Exception e) {
                Log.w(TAG, "Error in CrashListener.onCrashOccurred", e);
            }
        }
    }

    /**
     * 构建崩溃报告（包含应用、设备、异常等信息）
     *
     * @param ex 异常实例
     * @return 完整崩溃日志字符串
     */
    @SuppressLint({"SimpleDateFormat", "ObsoleteSdkInt"})
    private String buildCrashReport(Throwable ex) {
        StringBuilder sb = new StringBuilder(6144);
        final Locale reportLocale = Locale.US;
        final String sectionSeparator = "\n――――――――――――――――――――――――――――――\n";

        try {
            // 1. 报告头部
            sb.append("〓〓〓〓〓〓〓〓 Crash Report 〓〓〓〓〓〓〓〓\n\n");
            // 2. 应用信息
            appendApplicationInfo(sb, reportLocale);
            // 3. 设备信息
            sb.append(sectionSeparator).append("DEVICE INFO\n");
            appendDeviceInfo(sb, reportLocale);
            // 4. 运行时状态（使用ActivityLifecycleManager获取Activity信息）
            sb.append(sectionSeparator).append("RUNTIME STATE\n");
            appendRuntimeState(sb, reportLocale, new Date());
            // 5. 异常信息
            sb.append(sectionSeparator).append("EXCEPTION TRACE\n");
            appendExceptionInfo(sb, ex, reportLocale);
            // 6. 线程堆栈
            sb.append(sectionSeparator).append("THREAD STACKS\n");
            appendThreadStacks(sb);
            // 7. 内存信息
            sb.append(sectionSeparator).append("MEMORY STATUS\n");
            appendMemoryInfo(sb, reportLocale);
            // 8. 诊断信息
            sb.append(sectionSeparator).append("DIAGNOSTIC INFO\n");
            appendDiagnosticInfo(sb, reportLocale);

        } catch (Exception e) {
            Log.e(TAG, "Error building crash report", e);
            sb.append("\n!! REPORT GENERATION ERROR: ").append(e.getMessage());
        }

        final int MAX_LOG_LENGTH = 65536;
        if (sb.length() > MAX_LOG_LENGTH) {
            sb.setLength(MAX_LOG_LENGTH - 50);
            sb.append("\n[LOG TRUNCATED - EXCEEDED MAX LENGTH]");
        }

        return sb.toString();
    }

    private void appendApplicationInfo(StringBuilder sb, Locale locale) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            sb.append(String.format(locale, "%-10s: %s (%s)\n", "App", mAppName, getVersionName())).append(String.format(locale, "%-10s: %d\n", "VersionCode", pInfo.versionCode)).append(String.format(locale, "%-10s: %s\n", "BuildTime", getBuildTimestamp(pInfo))).append(String.format(locale, "%-10s: %b\n", "TrackActivities", mTrackActivities));
        } catch (Exception e) {
            appendError(sb, "ApplicationInfo", e);
        }
    }

    private void appendDeviceInfo(StringBuilder sb, Locale locale) {
        try {
            sb.append(String.format(locale, "%-10s: %s %s\n", "Model", Build.MANUFACTURER, Build.MODEL)).append(String.format(locale, "%-10s: Android %s (API %d)\n", "OS", Build.VERSION.RELEASE, Build.VERSION.SDK_INT)).append(String.format(locale, "%-10s: %s\n", "ABI", TextUtils.join(", ", Build.SUPPORTED_ABIS))).append(String.format(locale, "%-10s: %s\n", "Display", getScreenResolution())).append(String.format(locale, "%-10s: %s\n", "Density", getScreenDensity()));
        } catch (Exception e) {
            appendError(sb, "DeviceInfo", e);
        }
    }

    private void appendRuntimeState(StringBuilder sb, Locale locale, Date crashTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", locale);
            sb.append(String.format(locale, "%-14s: %s\n", "CrashTime", sdf.format(crashTime))).append(String.format(locale, "%-14s: %s\n", "Process", getProcessName())).append(String.format(locale, "%-14s: %d\n", "PID", Process.myPid())).append(String.format(locale, "%-14s: %s\n", "Uptime", formatUptime())).append(String.format(locale, "%-14s: %s\n", "Foreground", mActivityLifecycleManager.isForeground() ? "Yes" : "No")).append(String.format(locale, "%-14s: %d\n", "AliveActivities", mTrackActivities ? mActivityLifecycleManager.getActivities().size() : 0));
        } catch (Exception e) {
            appendError(sb, "RuntimeState", e);
        }
    }

    private void appendExceptionInfo(StringBuilder sb, Throwable ex, Locale locale) {
        try {
            Throwable rootCause = ex;
            int depth = 0;
            while (rootCause.getCause() != null && depth < 5) {
                rootCause = rootCause.getCause();
                depth++;
            }

            sb.append("Primary Exception:\n");
            appendThrowable(sb, ex, locale);

            if (rootCause != ex) {
                sb.append("\nRoot Cause (depth=").append(depth).append("):\n");
                appendThrowable(sb, rootCause, locale);
            }
        } catch (Exception e) {
            appendError(sb, "ExceptionInfo", e);
        }
    }

    private void appendThrowable(StringBuilder sb, Throwable t, Locale locale) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString().replace(mContext.getPackageName(), "[PKG]").replace("\tat ", "    ");

        sb.append(String.format(locale, "%s: %s\n", t.getClass().getSimpleName(), t.getMessage())).append(stackTrace);
    }

    private void appendThreadStacks(StringBuilder sb) {
        try {
            Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
                Thread thread = entry.getKey();
                if (thread.getState() != Thread.State.WAITING || entry.getValue().length > 0) {
                    sb.append("\n").append(thread.getName()).append(" [").append(thread.getState()).append("]").append(thread.isDaemon() ? " DAEMON" : "").append(" (prio=").append(thread.getPriority()).append(")");

                    int maxDepth = Math.min(entry.getValue().length, 6);
                    for (int i = 0; i < maxDepth; i++) {
                        sb.append("\n    ").append(entry.getValue()[i].toString().replace(mContext.getPackageName(), "[PKG]"));
                    }
                }
            }
        } catch (Exception e) {
            appendError(sb, "ThreadDump", e);
        }
    }

    private void appendMemoryInfo(StringBuilder sb, Locale locale) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();

            sb.append(String.format(locale, "%-10s: %.1f MB\n", "Used", usedMemory / 1048576f)).append(String.format(locale, "%-10s: %.1f MB\n", "Free", runtime.freeMemory() / 1048576f)).append(String.format(locale, "%-10s: %.1f MB\n", "Max", runtime.maxMemory() / 1048576f));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
                Debug.getMemoryInfo(memInfo);
                sb.append(String.format(locale, "%-10s: %d KB\n", "NativeHeap", memInfo.nativePss));
            }
        } catch (Exception e) {
            appendError(sb, "MemoryInfo", e);
        }
    }

    private void appendDiagnosticInfo(StringBuilder sb, Locale locale) {
        try {
            sb.append(String.format(locale, "%-10s: %s\n", "Network", getNetworkType())).append(String.format(locale, "%-10s: %d%%\n", "Battery", getBatteryLevel())).append(String.format(locale, "%-10s: %s\n", "Storage", getStorageState()));
        } catch (Exception e) {
            appendError(sb, "DiagnosticInfo", e);
        }
    }

    private String getBuildTimestamp(PackageInfo pInfo) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(pInfo.lastUpdateTime));
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String getProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        } else {
            try {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                    if (processes != null) {
                        int pid = Process.myPid();
                        for (ActivityManager.RunningAppProcessInfo process : processes) {
                            if (process.pid == pid) {
                                return process.processName;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return "unknown";
        }
    }

    private String getScreenResolution() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return String.format(Locale.US, "%dx%d", metrics.widthPixels, metrics.heightPixels);
    }

    private String getScreenDensity() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return String.format(Locale.US, "%ddpi (%.1fx)", metrics.densityDpi, metrics.density);
    }

    private String formatUptime() {
        long uptime = SystemClock.elapsedRealtime();
        return String.format(Locale.US, "%dd %02d:%02d:%02d.%03d", TimeUnit.MILLISECONDS.toDays(uptime), TimeUnit.MILLISECONDS.toHours(uptime) % 24, TimeUnit.MILLISECONDS.toMinutes(uptime) % 60, TimeUnit.MILLISECONDS.toSeconds(uptime) % 60, uptime % 1000);
    }

    private void appendError(StringBuilder sb, String section, Exception e) {
        sb.append("\n!! Error collecting ").append(section).append(": ").append(e.getClass().getSimpleName()).append(" - ").append(e.getMessage()).append("\n");
    }

    private String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";

        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return "Disconnected";

        return info.getTypeName() + " (" + info.getSubtypeName() + ")";
    }

    private int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) return -1;

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (scale <= 0) return -1;

        return (int) ((level / (float) scale) * 100);
    }

    private String getStorageState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] dirs = mContext.getExternalFilesDirs(null);
            if (dirs.length > 0 && dirs[0] != null) {
                return Environment.getExternalStorageState(dirs[0]);
            }
        }
        return Environment.getExternalStorageState();
    }

    private void saveCrashLog(String content) {
        if (!mSaveLogFile) {
            Log.d(TAG, "保存崩溃日志已禁用，跳过保存。");
            return;
        }

        String fileName = mAppName.replace(" ", "_") + "_CrashLog_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date()) + ".log";

        if (mCustomLogSavePath != null) {
            saveCrashLogToCustomPath(fileName, content);
            cleanOldLogsInCustomPath();
            return;
        }

        saveCrashLogToPrivateDir(fileName, content);
        cleanOldLogsInPrivateDir();
    }

    private void saveCrashLogToPrivateDir(String fileName, String content) {
        File dir = new File(mContext.getFilesDir(), CRASH_LOG_SUBDIR);
        if (!dir.exists()) dir.mkdirs();
        File logFile = new File(dir, fileName);
        try (FileWriter w = new FileWriter(logFile)) {
            w.write(content);
            Log.d(TAG, "Crash log saved to private dir: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing crash log to private dir", e);
        }
    }

    private void cleanOldLogsInPrivateDir() {
        if (mMaxLogFiles <= 0) return;
        File dir = new File(mContext.getFilesDir(), CRASH_LOG_SUBDIR);
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        List<File> list = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith(mAppName.replace(" ", "_") + "_CrashLog_")) {
                list.add(f);
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        int deleteCount = list.size() - mMaxLogFiles;
        for (int i = 0; i < deleteCount; i++) {
            if (list.get(i).delete()) {
                Log.d(TAG, "Deleted old private log: " + list.get(i).getAbsolutePath());
            }
        }
    }

    private void saveCrashLogToCustomPath(String fileName, String content) {
        File dir = mCustomLogSavePath;
        if (!dir.exists()) dir.mkdirs();
        File logFile = new File(dir, fileName);
        boolean isPublic = Environment.isExternalStorageEmulated() && (dir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()));
        if (isPublic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCrashLogUsingMediaStore(fileName, content);
        } else {
            try (FileWriter w = new FileWriter(logFile)) {
                w.write(content);
                Log.d(TAG, "Crash log saved to custom path: " + logFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Error writing crash log to custom path", e);
            }
        }
    }

    private void cleanOldLogsInCustomPath() {
        if (mMaxLogFiles <= 0 || mCustomLogSavePath == null) return;
        File dir = mCustomLogSavePath;
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        List<File> list = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith(mAppName.replace(" ", "_") + "_CrashLog_")) {
                list.add(f);
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        int deleteCount = list.size() - mMaxLogFiles;
        for (int i = 0; i < deleteCount; i++) {
            if (list.get(i).delete()) {
                Log.d(TAG, "Deleted old custom log: " + list.get(i).getAbsolutePath());
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private void saveCrashLogUsingMediaStore(String fileName, String content) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + mAppName + "/" + CRASH_LOG_SUBDIR);

        try {
            Uri uri = mContext.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Log.e(TAG, "Failed to insert MediaStore record");
                return;
            }

            try (OutputStream os = mContext.getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    Log.e(TAG, "Failed to open output stream for MediaStore");
                    return;
                }
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Log.d(TAG, "Crash log saved to MediaStore: " + uri);
            } catch (IOException e) {
                Log.e(TAG, "Error writing crash log to MediaStore", e);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when accessing MediaStore", e);
        }
    }

    private void launchCrashActivity(String crashLog) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> launchCrashActivity(crashLog));
            return;
        }

        Intent intent = new Intent(mContext, mCrashActivity);
        intent.putExtra("crash_log", crashLog);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start crash display activity", e);
            exitProcess();
        }
    }

    public void exitProcess() {
        try {
            for (CrashListener listener : mListeners) {
                try {
                    listener.beforeExit();
                } catch (Exception e) {
                    Log.w(TAG, "Error in CrashListener.beforeExit", e);
                }
            }

            if (mTrackActivities) {
                mActivityLifecycleManager.finishAllActivities();
                SystemClock.sleep(100);
            }

            Process.killProcess(Process.myPid());
            System.exit(1);
        } catch (Exception e) {
            Log.e(TAG, "Failed to exit process", e);
        }
    }

    private String getVersionName() {
        try {
            return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getAppName(Context context) {
        try {
            return context.getString(context.getApplicationInfo().labelRes);
        } catch (Exception e) {
            return "App";
        }
    }

    public boolean isShowRestart() {
        return mShowRestart;
    }

    public boolean isShowCopy() {
        return mShowCopy;
    }

    public boolean isShowCrashInfo() {
        return mShowCrashInfo;
    }

    public Class<? extends Activity> getRestartActivity() {
        return mRestartActivity != null ? mRestartActivity : getLaunchActivity();
    }

    public List<CrashListener> getListeners() {
        return new ArrayList<>(mListeners);
    }

    /**
     * 获取应用启动Activity
     *
     * @return 启动Activity的Class
     */
    private Class<? extends Activity> getLaunchActivity() {
        try {
            Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
            if (launchIntent != null && launchIntent.getComponent() != null) {
                String className = launchIntent.getComponent().getClassName();
                return (Class<? extends Activity>) Class.forName(className);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get launch activity", e);
        }
        return null;
    }
}