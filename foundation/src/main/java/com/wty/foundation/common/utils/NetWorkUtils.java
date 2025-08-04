package com.wty.foundation.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.wty.foundation.common.init.AppContext;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Author: 吴天宇
 * Date: 2025/8/4 11:03
 * Description: 网络工具类-提供网络状态检测、网络类型判断、IP地址获取等功能
 */
public class NetWorkUtils {
    private static final String TAG = "NetWorkUtil";

    /**
     * 2G网络类型定义
     */
    public static final int[] NETWORK_2G_DEFINITION = {TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_GSM, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN};

    /**
     * 3G网络类型定义
     */
    public static final int[] NETWORK_3G_DEFINITION = {TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA};

    /**
     * 4G网络类型定义 - 移除了不存在的NETWORK_TYPE_LTE_CA
     */
    public static final int[] NETWORK_4G_DEFINITION = {TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN};

    /**
     * 获取设备IP地址（优先返回IPv4）
     *
     * @return 设备的IP地址，优先返回IPv4，其次返回IPv6，获取失败返回null
     */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        // 优先返回IPv4地址
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                        // 其次考虑IPv6地址
                        else if (inetAddress instanceof Inet6Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "获取本地IP地址失败", ex);
        }
        return null;
    }

    /**
     * 获取网络连接类型
     *
     * @return 网络类型字符串，可能的值包括："WIFI"、"2G"、"3G"、"4G"、"5G"、"NONE"、"UNKNOWN"
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static String getConnNetworkType2() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return "";
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "UNKNOWN";

            // Android 10+ 使用新API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return "NONE";

                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps == null) return "UNKNOWN";

                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WIFI";
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return getCellularNetworkType(context);
                }
            }
            // Android 4.4-9.0 使用旧API
            else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info == null || !info.isConnected()) {
                    return info != null && info.isConnected() ? "UNKNOWN" : "NONE";
                }

                if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    return getMobileNetworkType(info.getSubtype());
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "缺少网络权限", e);
        }
        return "UNKNOWN";
    }

    /**
     * 获取移动网络类型（Android 10+）
     *
     * @return 移动网络类型字符串，可能的值包括："2G"、"3G"、"4G"、"5G"、"UNKNOWN"
     */
    @SuppressLint({"NewApi", "MissingPermission"})
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    private static String getCellularNetworkType(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return "UNKNOWN";

        int networkType = tm.getDataNetworkType();
        return getMobileNetworkType(networkType);
    }

    /**
     * 根据网络子类型获取网络类型
     *
     * @param subType 网络子类型值
     * @return 网络类型字符串，可能的值包括："2G"、"3G"、"4G"、"5G"、"UNKNOWN"
     */
    private static String getMobileNetworkType(int subType) {
        if (doFilter(subType, NETWORK_2G_DEFINITION)) return "2G";
        if (doFilter(subType, NETWORK_3G_DEFINITION)) return "3G";
        if (doFilter(subType, NETWORK_4G_DEFINITION)) return "4G";
        if (is5G(subType)) return "5G";
        return "UNKNOWN";
    }

    /**
     * 判断是否为5G网络
     *
     * @param subType 网络子类型值
     * @return true表示是5G网络，false表示不是
     */
    private static boolean is5G(int subType) {
        // 标准5G类型判断（API 29+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return subType == TelephonyManager.NETWORK_TYPE_NR;
        }
        // 兼容旧设备（某些厂商自定义值）
        return subType == 20; // NETWORK_TYPE_NR 值
    }

    /**
     * 检查网络是否可用
     *
     * @return true表示网络可用，false表示网络不可用
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isNetworkAvailable() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null");
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        try {
            // Android 10+ 使用新API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return false;

                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            // Android 4.4-9.0 使用旧API
            else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "缺少网络权限", e);
            return false;
        }
    }

    /**
     * 数组过滤工具，检查key是否存在于data数组中
     *
     * @param key  要检查的值
     * @param data 目标数组
     * @return true表示key存在于data数组中，false表示不存在
     */
    private static boolean doFilter(int key, int[] data) {
        for (int s : data) {
            if (key == s) {
                return true;
            }
        }
        return false;
    }
}