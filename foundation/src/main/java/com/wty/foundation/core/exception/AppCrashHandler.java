package com.wty.foundation.core.exception;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppCrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "AppCrashHandler";
    private static final int DEFAULT_MAX_LOG_FILES = 20;
    private static final String CRASH_LOG_SUBDIR = "CrashLogs";
    private static AppCrashHandler instance;

    private final Context mContext;
    private final Thread.UncaughtExceptionHandler mDefaultHandler;
    private final String mAppName;
    private final List<CrashListener> mListeners = new ArrayList<>();

    // 配置参数
    private int mMaxLogFiles = DEFAULT_MAX_LOG_FILES;
    private boolean mEnabled = true;
    private boolean mShowRestart = true;
    private boolean mShowCopy = true;
    private boolean mShowCrashInfo = false;
    private boolean mShowCrashActivity = true;
    private boolean mSaveLogFile = false;
    private Class<? extends Activity> mRestartActivity;
    private Class<? extends Activity> mCrashActivity = CrashDisplayActivity.class;

    public interface CrashListener {
        void onCrashOccurred(Thread thread, Throwable ex, String crashLog);

        default void beforeRestart() {
        }

        default void beforeExit() {
        }
    }

    private AppCrashHandler(Context context) {
        mContext = context.getApplicationContext();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        mAppName = getAppName(context);
    }

    public static AppCrashHandler getInstance() {
        return instance;
    }

    public static synchronized Configurator initCrashHandler(Context context) {
        if (instance == null) {
            instance = new AppCrashHandler(context);
            Thread.setDefaultUncaughtExceptionHandler(instance);
        }
        return new Configurator(instance);
    }

    public static class Configurator {
        private final AppCrashHandler mHandler;

        private Configurator(AppCrashHandler handler) {
            mHandler = handler;
        }

        public Configurator setMaxLogFiles(int max) {
            mHandler.mMaxLogFiles = Math.max(1, max);
            return this;
        }

        public Configurator setRestartActivity(Class<? extends Activity> activity) {
            mHandler.mRestartActivity = activity;
            return this;
        }

        public Configurator setCrashActivity(Class<? extends Activity> activity) {
            mHandler.mCrashActivity = activity;
            return this;
        }

        public Configurator setShowButtons(boolean showRestart, boolean showCopy) {
            mHandler.mShowRestart = showRestart;
            mHandler.mShowCopy = showCopy;
            return this;
        }

        public Configurator setEnabled(boolean enabled) {
            mHandler.mEnabled = enabled;
            return this;
        }

        public Configurator setShowCrashInfo(boolean show) {
            mHandler.mShowCrashInfo = show;
            return this;
        }

        public Configurator setShowCrashActivity(boolean show) {
            mHandler.mShowCrashActivity = show;
            return this;
        }

        public Configurator setSaveLogFile(boolean save) {
            mHandler.mSaveLogFile = save;
            return this;
        }

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

    private void handleMainThreadCrash(Thread thread, Throwable ex) {
        try {
            final String crashLog = buildCrashReport(ex);
            notifyCrashOccurred(thread, ex, crashLog);

            if (mShowCrashActivity) {
                launchCrashActivity(crashLog);
                new Thread(() -> {
                    saveCrashLog(crashLog);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> Looper.loop(), 100);
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
            Log.e(TAG, "Crash handling failed", e);
            exitProcess();
        }
    }

    private void notifyCrashOccurred(Thread thread, Throwable ex, String log) {
        for (CrashListener listener : mListeners) {
            try {
                listener.onCrashOccurred(thread, ex, log);
            } catch (Exception e) {
                Log.w(TAG, "onCrashOccurred listener error", e);
            }
        }
    }

    @SuppressLint({"SimpleDateFormat", "ObsoleteSdkInt"})
    private String buildCrashReport(Throwable ex) {
        StringBuilder sb = new StringBuilder(6144);
        final Locale reportLocale = Locale.US;
        final String sectionSeparator = "\n――――――――――――――――――――――――――――――\n";

        try {
            // 1. 应用基本信息
            sb.append("〓〓〓〓〓〓〓〓 Crash Report 〓〓〓〓〓〓〓〓\n\n");
            appendApplicationInfo(sb, reportLocale);

            // 2. 设备信息
            sb.append(sectionSeparator).append("DEVICE INFO\n");
            appendDeviceInfo(sb, reportLocale);

            // 3. 运行时状态
            sb.append(sectionSeparator).append("RUNTIME STATE\n");
            appendRuntimeState(sb, reportLocale, new Date());

            // 4. 异常信息
            sb.append(sectionSeparator).append("EXCEPTION TRACE\n");
            appendExceptionInfo(sb, ex, reportLocale);

            // 5. 线程堆栈
            sb.append(sectionSeparator).append("THREAD STACKS\n");
            appendThreadStacks(sb);

            // 6. 内存信息
            sb.append(sectionSeparator).append("MEMORY STATUS\n");
            appendMemoryInfo(sb, reportLocale);

            // 7. 诊断信息
            sb.append(sectionSeparator).append("DIAGNOSTIC INFO\n");
            appendDiagnosticInfo(sb, reportLocale);

        } catch (Exception e) {
            Log.e(TAG, "Error building crash report", e);
            sb.append("\n!! REPORT GENERATION ERROR: ").append(e);
        }

        final int MAX_LOG_LENGTH = 65536;
        if (sb.length() > MAX_LOG_LENGTH) {
            sb.setLength(MAX_LOG_LENGTH - 50);
            sb.append("\n[LOG TRUNCATED]");
        }

        return sb.toString();
    }

    private void appendApplicationInfo(StringBuilder sb, Locale locale) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            sb.append(String.format(locale, "%-10s: %s (%s)\n", "App", mAppName, getVersionName())).append(String.format(locale, "%-10s: %d\n", "Version", pInfo.versionCode)).append(String.format(locale, "%-10s: %s\n", "Build", getBuildTimestamp(pInfo)));
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
            sb.append(String.format(locale, "%-14s: %s\n", "Crash Time", sdf.format(crashTime))).append(String.format(locale, "%-14s: %s\n", "Process", getProcessName())).append(String.format(locale, "%-14s: %d\n", "PID", Process.myPid())).append(String.format(locale, "%-14s: %s\n", "System Uptime", formatUptime())).append(String.format(locale, "%-14s: %s\n", "Foreground", isAppInForeground() ? "Yes" : "No"));
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
                sb.append(String.format(locale, "%-10s: %d KB\n", "Native Heap", memInfo.nativePss));
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
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(pInfo.lastUpdateTime));
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
        return String.format(Locale.US, "%dd %02d:%02d:%02d.%03d", TimeUnit.MILLISECONDS.toDays(uptime), TimeUnit.MILLISECONDS.toHours(uptime) % 24, TimeUnit.MILLISECONDS.toMinutes(uptime) % 60, TimeUnit.MILLISECONDS.toSeconds(uptime) % 60, uptime % 1000); // 毫秒部分
    }

    private boolean isAppInForeground() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;

        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.processName.equals(mContext.getPackageName())) {
                return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveCrashLogUsingMediaStore(fileName, content);
            cleanOldLogsViaMediaStore();
        } else {
            if (hasWriteExternalStoragePermission()) {
                File logDir = getExternalCrashLogDir();
                File targetDir = logDir;

                if (logDir == null || (!logDir.exists() && !logDir.mkdirs())) {
                    File parentDir = logDir != null ? logDir.getParentFile() : null;
                    if (parentDir != null && (parentDir.exists() || parentDir.mkdirs())) {
                        targetDir = parentDir;
                    } else {
                        targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    }
                }

                File logFile = new File(targetDir, fileName);
                try (FileWriter writer = new FileWriter(logFile)) {
                    writer.write(content);
                    cleanOldLogsInPossibleDirectories();
                } catch (IOException e) {
                    Log.e(TAG, "Save log failed", e);
                }
            } else {
                Log.e(TAG, "No WRITE_EXTERNAL_STORAGE permission, cannot save crash log");
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
                Log.d(TAG, "Crash log saved via MediaStore: " + uri);
            } catch (IOException e) {
                Log.e(TAG, "Error writing crash log to MediaStore", e);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when accessing MediaStore", e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private void cleanOldLogsViaMediaStore() {
        if (mMaxLogFiles <= 0) {
            return;
        }

        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + mAppName + "/" + CRASH_LOG_SUBDIR;
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{MediaStore.Downloads._ID, MediaStore.Downloads.DATE_ADDED};
        String selection = MediaStore.Downloads.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{relativePath};
        String sortOrder = MediaStore.Downloads.DATE_ADDED + " ASC";

        try (Cursor cursor = mContext.getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {

            if (cursor == null) {
                return;
            }

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
            List<Uri> uris = new ArrayList<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                Uri uri = ContentUris.withAppendedId(collection, id);
                uris.add(uri);
            }

            int total = uris.size();
            if (total > mMaxLogFiles) {
                int deleteCount = total - mMaxLogFiles;
                for (int i = 0; i < deleteCount; i++) {
                    Uri uri = uris.get(i);
                    mContext.getContentResolver().delete(uri, null, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clean old logs via MediaStore", e);
        }
    }

    private void cleanOldLogsInPossibleDirectories() {
        if (mMaxLogFiles <= 0) {
            return;
        }

        List<File> possibleDirs = new ArrayList<>();
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadsDir, mAppName);
        File crashLogsDir = new File(appDir, CRASH_LOG_SUBDIR);

        possibleDirs.add(downloadsDir);
        possibleDirs.add(appDir);
        possibleDirs.add(crashLogsDir);

        String prefix = mAppName.replace(" ", "_") + "_CrashLog_";
        List<File> allLogFiles = new ArrayList<>();

        for (File dir : possibleDirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith(prefix)) {
                            allLogFiles.add(file);
                        }
                    }
                }
            }
        }

        Collections.sort(allLogFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });

        int totalFiles = allLogFiles.size();
        if (totalFiles > mMaxLogFiles) {
            int deleteCount = totalFiles - mMaxLogFiles;
            for (int i = 0; i < deleteCount; i++) {
                File file = allLogFiles.get(i);
                if (file.exists() && !file.delete()) {
                    Log.w(TAG, "Failed to delete: " + file.getAbsolutePath());
                }
            }
            Log.d(TAG, "Deleted " + deleteCount + " old crash logs");
        }
    }

    private File getExternalCrashLogDir() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadsDir, mAppName);
        return new File(appDir, CRASH_LOG_SUBDIR);
    }

    private boolean hasWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
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
            Log.e(TAG, "Start crash activity failed", e);
            exitProcess();
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

    public void exitProcess() {
        try {
            Process.killProcess(Process.myPid());
            System.exit(1);
        } catch (Exception e) {
            Log.e(TAG, "Exit process failed", e);
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

    private Class<? extends Activity> getLaunchActivity() {
        try {
            String className = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName()).getComponent().getClassName();
            return (Class<? extends Activity>) Class.forName(className);
        } catch (Exception e) {
            return null;
        }
    }
}