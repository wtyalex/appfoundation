package com.wty.foundation.common.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.wty.foundation.common.init.AppContext;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppUtils {
    private static final String TAG = "AppUtils";
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取应用程序上下文
     *
     * @return 应用程序上下文，若为空则日志报错
     */
    private static Context getContext() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "严重错误：全局应用上下文为空，请检查 AppContext 初始化");
        }
        return context;
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用名称，获取失败则返回空字符串
     */
    public static String getAppName() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppName() 中止：上下文为空");
            return "";
        }

        Object cached = CACHE.get("appName");
        if (cached instanceof String) {
            return (String) cached;
        }

        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            String appName = pm.getApplicationLabel(appInfo).toString();
            CACHE.put("appName", appName);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名对应的应用名称：" + context.getPackageName(), e);
        } catch (Exception e) {
            Log.e(TAG, "获取应用名称时出现意外错误", e);
        }
        return "";
    }

    /**
     * 获取版本名称
     *
     * @return 版本名称，获取失败则返回空字符串
     */
    public static String getVersionName() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getVersionName() 中止：上下文为空");
            return "";
        }

        Object cached = CACHE.get("versionName");
        if (cached instanceof String) {
            return (String) cached;
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = pkgInfo.versionName != null ? pkgInfo.versionName : "";
            CACHE.put("versionName", version);
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return "";
    }

    /**
     * 获取版本号
     *
     * @return 版本号，获取失败则返回 -1L
     */
    public static long getVersionCode() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getVersionCode() 中止：上下文为空");
            return -1L;
        }

        Object cached = CACHE.get("versionCode");
        if (cached instanceof Long) {
            return (Long) cached;
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long code;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                code = pkgInfo.getLongVersionCode();
            } else {
                code = pkgInfo.versionCode;
            }

            CACHE.put("versionCode", code);
            return code;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return -1L;
    }

    /**
     * 获取包名
     *
     * @return 包名，上下文为空则返回空字符串
     */
    public static String getPackageName() {
        Context context = getContext();
        return context != null ? context.getPackageName() : "";
    }

    /**
     * 获取应用图标
     *
     * @return 应用图标对应的 Bitmap，获取失败则返回空 Bitmap
     */
    public static Bitmap getAppIconBitmap() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppIconBitmap() 中止：上下文为空");
            return createEmptyBitmap();
        }

        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(context.getPackageName());
            return drawableToBitmap(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名对应的应用图标：" + context.getPackageName(), e);
        }
        return createEmptyBitmap();
    }

    /**
     * 创建空位图
     *
     * @return 一个 1x1 的 ARGB_8888 格式的空 Bitmap
     */
    private static Bitmap createEmptyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    /**
     * 将 Drawable 转换为 Bitmap
     *
     * @param drawable 要转换的 Drawable 对象
     * @return 转换后的 Bitmap 对象
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 获取安装时间（毫秒级精度）
     *
     * @return 安装时间，获取失败则返回 -1L
     */
    public static long getAppInstallTime() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppInstallTime() 中止：上下文为空");
            return -1L;
        }

        Object cached = CACHE.get("appInstallTime");
        if (cached instanceof Long) {
            return (Long) cached;
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long time = pkgInfo.firstInstallTime;
            CACHE.put("appInstallTime", time);
            return time;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return -1L;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间，获取失败则返回 -1L
     */
    public static long getAppUpdateTime() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppUpdateTime() 中止：上下文为空");
            return -1L;
        }

        Object cached = CACHE.get("appUpdateTime");
        if (cached instanceof Long) {
            return (Long) cached;
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long time = pkgInfo.lastUpdateTime;
            CACHE.put("appUpdateTime", time);
            return time;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return -1L;
    }

    /**
     * 判断是否为系统应用
     *
     * @return 是否为系统应用，获取失败则返回 false
     */
    public static boolean isSystemApp() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "isSystemApp() 中止：上下文为空");
            return false;
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return false;
    }

    /**
     * 获取应用签名
     *
     * @return 应用签名的 MD5 值，获取失败则返回空字符串
     */
    public static String getAppSignature() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppSignature() 中止：上下文为空");
            return "";
        }

        Object cached = CACHE.get("appSignature");
        if (cached instanceof String) {
            return (String) cached;
        }

        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            if (pkgInfo.signatures == null || pkgInfo.signatures.length == 0) {
                Log.e(TAG, "未找到应用包名对应的签名：" + context.getPackageName());
                return "";
            }

            // 优先使用 V2 及以上签名方案
            Signature signature = pkgInfo.signatures[0];
            String md5 = calculateMd5(signature.toByteArray());
            CACHE.put("appSignature", md5);
            return md5;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return "";
    }

    /**
     * 计算 MD5 值
     *
     * @param data 要计算 MD5 的字节数组
     * @return MD5 字符串，计算失败则返回空字符串
     */
    private static String calculateMd5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xFF & b);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 算法不可用", e);
        }
        return "";
    }

    /**
     * 卸载应用
     *
     * @param packageName 要卸载应用的包名
     */
    public static void uninstallApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            Log.e(TAG, "uninstallApp() 失败：包名不能为空");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "uninstallApp() 中止：上下文为空");
            return;
        }

        try {
            // 检查是否具有请求卸载应用的权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少请求卸载应用的权限");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "未找到用于卸载包名对应的应用的活动：" + packageName);
            }
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "未找到用于卸载包名对应的应用的活动：" + packageName, e);
        } catch (SecurityException e) {
            Log.e(TAG, "卸载应用时出现安全异常：" + packageName, e);
        }
    }

    /**
     * 安装 APK，通过 Uri
     *
     * @param context 上下文对象
     * @param apkUri  APK 文件的 Uri
     */
    public static void installApk(Context context, Uri apkUri) {
        if (context == null || apkUri == null) {
            Log.e(TAG, "installApk() 失败：参数无效");
            return;
        }

        try {
            // 检查是否具有请求安装应用的权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少请求安装应用的权限");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 对于 N 及以上版本需要特别处理 Uri 权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "未找到处理 APK 安装的活动");
            }
        } catch (Exception e) {
            Log.e(TAG, "APK 安装失败，Uri 为：" + apkUri, e);
        }
    }

    /**
     * 安装 APK，通过 File
     *
     * @param context 上下文对象
     * @param apkFile APK 文件对象
     */
    public static void installApk(Context context, File apkFile) {
        // 检查参数有效性和文件是否存在
        if (context == null || apkFile == null || !apkFile.exists()) {
            Log.e(TAG, "installApk() 失败：参数无效或文件不存在");
            return;
        }

        try {
            // 检查是否具有请求安装应用的权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少请求安装应用的权限");
                return;
            }

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 对于 Android 7.0 及以上版本，使用 FileProvider 获取 content URI
                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);
            } else {
                // 对于 Android 7.0 以下版本，使用 Uri.fromFile
                apkUri = Uri.fromFile(apkFile);
            }

            // 创建安装 APK 的意图
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 为 Android 7.0 及以上版本添加读取 URI 的权限
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // 查询能够处理该意图的活动
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    // 为每个能够处理该意图的应用授予读取 URI 的权限
                    context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            // 检查是否有活动可以处理该意图
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                // 启动安装活动
                context.startActivity(intent);
            } else {
                Log.e(TAG, "未找到处理 APK 安装的活动");
            }
        } catch (Exception e) {
            Log.e(TAG, "APK 安装失败，文件路径为：" + apkFile.getAbsolutePath(), e);
        }
    }

    /**
     * 获取已安装应用列表
     *
     * @param includeSystemApps 是否包含系统应用，true 表示包含，false 表示不包含
     * @return 已安装应用的列表，获取失败则返回空列表
     */
    public static List<ApplicationInfo> getAllInstalledApps(boolean includeSystemApps) {
        // 获取上下文对象
        Context context = getContext();
        // 检查上下文对象是否为空
        if (context == null) {
            Log.w(TAG, "getAllInstalledApps() 中止：上下文为空");
            return Collections.emptyList();
        }

        // 获取包管理器
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps;

        try {
            // 获取所有已安装应用的信息
            apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            Log.e(TAG, "获取已安装应用信息失败", e);
            return Collections.emptyList();
        }

        // 如果需要包含系统应用，直接返回所有应用列表
        if (includeSystemApps) {
            return new ArrayList<>(apps);
        }

        // 过滤出用户应用
        List<ApplicationInfo> filtered = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            // 判断是否为用户应用而非系统应用
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                filtered.add(app);
            }
        }
        return filtered;
    }
}