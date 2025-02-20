package com.wty.foundation.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.wty.foundation.common.init.AppContext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppUtils {
    private static final String TAG = "AppUtils";
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取应用程序名称
     *
     * @return 应用程序名称，获取失败时返回空字符串 ""
     */
    public static String getAppName() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return "";
        }
        if (CACHE.containsKey("appName")) {
            return (String) CACHE.get("appName");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            String appName = context.getResources().getString(labelRes);
            CACHE.put("appName", appName);
            return appName;
        } catch (Exception e) {
            Log.e(TAG, "getAppName failed", e);
        }
        return "";
    }

    /**
     * 获取应用程序版本名称
     *
     * @return 应用程序版本名称，获取失败时返回空字符串 ""
     */
    public static String getVersionName() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return "";
        }
        if (CACHE.containsKey("versionName")) {
            return (String) CACHE.get("versionName");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            CACHE.put("versionName", versionName);
            return versionName;
        } catch (Exception e) {
            Log.e(TAG, "getVersionName failed", e);
        }
        return "";
    }

    /**
     * 获取应用程序版本号
     *
     * @return 应用程序版本号，获取失败时返回 -1
     */
    public static int getVersionCode() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return -1;
        }
        if (CACHE.containsKey("versionCode")) {
            return (int) CACHE.get("versionCode");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            CACHE.put("versionCode", versionCode);
            return versionCode;
        } catch (Exception e) {
            Log.e(TAG, "getVersionCode failed", e);
        }
        return -1;
    }

    /**
     * 获取应用程序包名
     *
     * @return 应用程序包名，获取失败时返回空字符串 ""
     */
    public static String getPackageName() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return "";
        }
        return context.getPackageName();
    }

    /**
     * 获取应用程序图标 Bitmap
     *
     * @return 应用程序图标 Bitmap，获取失败时返回空 Bitmap
     */
    public static Bitmap getAppIconBitmap() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // 返回一个空 Bitmap
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            Drawable d = packageManager.getApplicationIcon(applicationInfo);
            if (d instanceof BitmapDrawable) {
                return ((BitmapDrawable) d).getBitmap();
            }
            // 如果不是 BitmapDrawable，尝试将 Drawable 转换为 Bitmap
            Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            d.draw(new android.graphics.Canvas(bitmap));
            return bitmap;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getAppIconBitmap failed", e);
        }
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // 返回一个空 Bitmap
    }

    /**
     * 获取应用安装时间
     *
     * @return 应用安装时间（毫秒），获取失败时返回 -1
     */
    public static long getAppInstallTime() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return -1;
        }
        if (CACHE.containsKey("appInstallTime")) {
            return (long) CACHE.get("appInstallTime");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            long installTime = packageInfo.firstInstallTime;
            CACHE.put("appInstallTime", installTime);
            return installTime;
        } catch (Exception e) {
            Log.e(TAG, "getAppInstallTime failed", e);
        }
        return -1;
    }

    /**
     * 获取应用更新时间
     *
     * @return 应用更新时间（毫秒），获取失败时返回 -1
     */
    public static long getAppUpdateTime() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return -1;
        }
        if (CACHE.containsKey("appUpdateTime")) {
            return (long) CACHE.get("appUpdateTime");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            long updateTime = packageInfo.lastUpdateTime;
            CACHE.put("appUpdateTime", updateTime);
            return updateTime;
        } catch (Exception e) {
            Log.e(TAG, "getAppUpdateTime failed", e);
        }
        return -1;
    }

    /**
     * 判断应用是否为系统应用
     *
     * @return true：系统应用，false：非系统应用，获取失败时返回 false
     */
    public static boolean isSystemApp() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return false;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception e) {
            Log.e(TAG, "isSystemApp failed", e);
        }
        return false;
    }

    /**
     * 获取应用签名信息
     *
     * @return 应用签名信息的 MD5 值，获取失败时返回空字符串 ""
     */
    public static String getAppSignature() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return "";
        }
        if (CACHE.containsKey("appSignature")) {
            return (String) CACHE.get("appSignature");
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            if (signatures != null && signatures.length > 0) {
                String signatureMd5 = getSignatureMd5(signatures[0]);
                CACHE.put("appSignature", signatureMd5);
                return signatureMd5;
            }
        } catch (Exception e) {
            Log.e(TAG, "getAppSignature failed", e);
        }
        return "";
    }

    /**
     * 将签名信息转换为 MD5 值
     *
     * @param signature 签名信息
     * @return MD5 值，转换失败时返回空字符串 ""
     */
    private static String getSignatureMd5(Signature signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getSignatureMd5 failed", e);
        }
        return "";
    }

    /**
     * 检查应用是否具有指定权限
     *
     * @param permission 权限名称
     * @return 是否具有指定权限
     */
    public static boolean checkPermission(String permission) {
        Context context = AppContext.getInstance().getContext();
        if (context == null || permission == null || permission.isEmpty()) {
            Log.e(TAG, "Context or permission is null");
            return false;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求安装未知来源的应用
     */
    public static void requestInstallFromUnknownSources() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 对于 Android 4.2 到 Android 7.1 版本
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * 卸载应用
     *
     * @param packageName 应用包名
     */
    public static void uninstallApp(String packageName) {
        Context context = AppContext.getInstance().getContext();
        if (context == null || packageName == null || packageName.isEmpty()) {
            Log.e(TAG, "Context or packageName is null");
            return;
        }
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(uninstallIntent);
    }

    /**
     * 安装APK文件
     * <p>
     * 在 Android 系统中安装 APK 文件，需要根据不同的 Android 版本和 APK 文件的存储位置，获取相应的权限：
     * <ul>
     *     <li>对于 Android 6.0（API 级别 23）及以上版本，如果 APK 文件存储在外部存储设备（如 SD 卡），
     *         需要在 AndroidManifest.xml 中声明 {@code <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>} 权限，
     *         并且在运行时动态请求该权限，以读取 APK 文件。</li>
     *     <li>对于 Android 8.0（API 级别 26）及以上版本，如果要安装来自未知来源（非官方应用商店）的应用，
     *         需要请求 {@code android.Manifest.permission.REQUEST_INSTALL_UNKNOWN_SOURCES} 权限，
     *         可以通过调用 Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES 来引导用户开启该权限。</li>
     *     <li>在 Android 7.0（API 级别 24）及以上版本，当使用 FileProvider 来共享 APK 文件的 Uri 时，
     *         还需要在 Intent 中设置 {@code Intent.FLAG_GRANT_READ_URI_PERMISSION} 标志，
     *         以授予目标应用读取该 Uri 对应的文件的权限。</li>
     * </ul>
     *
     * @param apkFileUri APK文件的Uri
     */
    public static void installApk(Uri apkFileUri) {
        Context context = AppContext.getInstance().getContext();
        if (context == null || apkFileUri == null) {
            Log.e(TAG, "Context or apkFileUri is null");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(apkFileUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 获取应用请求的权限列表
     *
     * @return 应用请求的权限列表，获取失败时返回空列表
     */
    public static List<String> getRequestedPermissions() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return Collections.emptyList();
        }
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                return Arrays.asList(packageInfo.requestedPermissions);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getRequestedPermissions failed", e);
        }
        return Collections.emptyList();
    }

    /**
     * 获取所有已安装的应用程序信息
     *
     * @return 所有已安装的应用程序信息列表，获取失败时返回空列表
     */
    public static List<ApplicationInfo> getAllInstalledApplications() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return Collections.emptyList();
        }
        PackageManager pm = context.getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }
}