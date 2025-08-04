package com.wty.foundation.common.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用工具类，提供与应用相关的常用操作
 * 包括获取应用信息、版本信息、签名信息、安装/卸载应用等功能
 */
public class AppUtils {
    private static final String TAG = "AppUtils";
    /**
     * 缓存容器，用于存储已获取的应用信息，提高访问效率
     * 键为缓存标识字符串，值为对应的缓存数据
     */
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取全局应用上下文
     *
     * @return 应用上下文对象，若未初始化则返回null
     */
    private static Context getContext() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "严重错误：全局应用上下文为空，请检查 AppContext 初始化");
        }
        return context;
    }

    /**
     * 获取当前应用的名称
     *
     * @return 应用名称字符串，获取失败时返回空字符串
     */
    public static String getAppName() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppName() 中止：上下文为空");
            return "";
        }

        // 从缓存获取，避免重复计算
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
     * 获取当前应用的版本名称
     *
     * @return 版本名称字符串，获取失败时返回空字符串
     */
    public static String getVersionName() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getVersionName() 中止：上下文为空");
            return "";
        }

        // 从缓存获取，避免重复计算
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
     * 获取当前应用的版本号
     * 适配Android P及以上版本的长版本号机制
     *
     * @return 版本号（long类型），获取失败时返回-1L
     */
    public static long getVersionCode() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getVersionCode() 中止：上下文为空");
            return -1L;
        }

        // 从缓存获取，避免重复计算
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
     * 获取当前设备的Android SDK版本号
     *
     * @return SDK版本号（int类型）
     */
    public static int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取当前应用的包名
     *
     * @return 应用包名字符串，上下文为空时返回空字符串
     */
    public static String getPackageName() {
        Context context = getContext();
        return context != null ? context.getPackageName() : "";
    }

    /**
     * 获取当前应用的图标 Bitmap
     *
     * @return 应用图标 Bitmap，获取失败时返回1x1的空Bitmap
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
     * 创建一个1x1的空Bitmap，作为获取图标失败时的默认返回值
     *
     * @return 1x1尺寸的空Bitmap
     */
    private static Bitmap createEmptyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    /**
     * 将Drawable转换为Bitmap
     *
     * @param drawable 待转换的Drawable对象
     * @return 转换后的Bitmap对象
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        // 如果已经是BitmapDrawable，直接获取Bitmap
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // 计算Drawable的宽高，确保至少为1px
        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        // 创建对应尺寸的Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // 绘制Drawable到Bitmap
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 获取应用的安装时间（毫秒时间戳）
     *
     * @return 安装时间戳，获取失败时返回-1L
     */
    public static long getAppInstallTime() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppInstallTime() 中止：上下文为空");
            return -1L;
        }

        // 从缓存获取，避免重复计算
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
     * 获取应用的最后更新时间（毫秒时间戳）
     *
     * @return 更新时间戳，获取失败时返回-1L
     */
    public static long getAppUpdateTime() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppUpdateTime() 中止：上下文为空");
            return -1L;
        }

        // 从缓存获取，避免重复计算
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
     * 判断当前应用是否为系统应用
     * 系统应用包括：预装系统应用和已更新的系统应用
     *
     * @return true表示是系统应用，false表示是用户应用，获取失败时返回false
     */
    public static boolean isSystemApp() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "isSystemApp() 中止：上下文为空");
            return false;
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            // 检查应用标志：系统应用或已更新的系统应用
            return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        }
        return false;
    }

    /**
     * 获取应用的签名信息（MD5值）
     * 适配Android P及以上版本的签名获取机制
     *
     * @return 签名的MD5字符串，获取失败时返回空字符串
     */
    public static String getAppSignature() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAppSignature() 中止：上下文为空");
            return "";
        }

        // 从缓存获取，避免重复计算
        Object cached = CACHE.get("appSignature");
        if (cached instanceof String) {
            return (String) cached;
        }

        try {
            Signature[] signatures = null;
            // 适配Android P及以上版本的签名获取方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signingInfo = pkgInfo.signingInfo;
                if (signingInfo != null) {
                    // 处理多签名情况
                    if (signingInfo.hasMultipleSigners()) {
                        signatures = signingInfo.getApkContentsSigners();
                    } else {
                        signatures = signingInfo.getSigningCertificateHistory();
                    }
                }
            } else {
                // 旧版系统的签名获取方式
                PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = pkgInfo.signatures;
            }

            if (signatures == null || signatures.length == 0) {
                Log.e(TAG, "未找到应用包名对应的签名：" + context.getPackageName());
                return "";
            }

            // 计算第一个签名的MD5值
            String md5 = MD5.calculateMd5(signatures[0].toByteArray());
            CACHE.put("appSignature", md5);
            return md5;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "未找到应用包名：" + context.getPackageName(), e);
        } catch (Exception e) {
            Log.e(TAG, "获取应用签名失败", e);
        }
        return "";
    }

    /**
     * 卸载指定包名的应用
     *
     * @param packageName 待卸载应用的包名，不能为空
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
            // 创建卸载意图
            Intent intent = new Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 检查是否有应用可以处理该意图
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
     * 通过Uri安装APK文件
     * 适配Android 7.0+的文件访问权限机制和Android 8.0+的安装权限
     *
     * @param context 上下文对象，不能为空
     * @param apkUri  APK文件的Uri，不能为空
     */
    public static void installApk(Context context, Uri apkUri) {
        if (context == null || apkUri == null) {
            Log.e(TAG, "installApk() 失败：参数无效");
            return;
        }

        try {
            // 检查Android 8.0+的安装未知应用权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少请求安装应用的权限");
                return;
            }

            // 创建安装意图
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addCategory(Intent.CATEGORY_DEFAULT);

            // Android 7.0+需要添加读取Uri权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            // 检查是否有应用可以处理该意图
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
     * 通过File对象安装APK文件
     * 内部会根据系统版本处理Uri转换（普通Uri或FileProvider Uri）
     *
     * @param context 上下文对象，不能为空
     * @param apkFile APK文件对象，不能为空且必须存在
     */
    public static void installApk(Context context, File apkFile) {
        if (context == null || apkFile == null || !apkFile.exists()) {
            Log.e(TAG, "installApk() 失败：参数无效或文件不存在");
            return;
        }

        try {
            // 检查Android 8.0+的安装未知应用权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Log.w(TAG, "缺少请求安装应用的权限");
                return;
            }

            Uri apkUri;
            // Android 7.0+使用FileProvider获取Uri，避免文件权限问题
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);
            } else {
                // 旧版系统直接使用文件Uri
                apkUri = Uri.fromFile(apkFile);
            }

            // 创建安装意图
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addCategory(Intent.CATEGORY_DEFAULT);

            // Android 7.0+需要添加读取Uri权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            // 检查是否有应用可以处理该意图
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "未找到处理 APK 安装的活动");
            }
        } catch (Exception e) {
            Log.e(TAG, "APK 安装失败，文件路径为：" + apkFile.getAbsolutePath(), e);
        }
    }

    /**
     * 获取设备上所有已安装的应用信息
     *
     * @param includeSystemApps 是否包含系统应用
     *                          true：返回所有应用（包括系统应用和用户应用）
     *                          false：仅返回用户应用
     * @return 应用信息列表，获取失败时返回空列表
     */
    public static List<ApplicationInfo> getAllInstalledApps(boolean includeSystemApps) {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "getAllInstalledApps() 中止：上下文为空");
            return Collections.emptyList();
        }

        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps;

        try {
            // 获取所有已安装应用的信息
            apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch (Exception e) {
            Log.e(TAG, "获取已安装应用信息失败", e);
            return Collections.emptyList();
        }

        // 如果需要包含系统应用，直接返回全部
        if (includeSystemApps) {
            return new ArrayList<>(apps);
        }

        // 过滤系统应用，仅保留用户应用
        List<ApplicationInfo> filtered = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            boolean isSystemApp = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isUpdatedSystemApp = (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

            // 非系统应用且非更新的系统应用，视为用户应用
            if (!isSystemApp && !isUpdatedSystemApp) {
                filtered.add(app);
            }
        }
        return filtered;
    }
}