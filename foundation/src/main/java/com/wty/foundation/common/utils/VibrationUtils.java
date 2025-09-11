package com.wty.foundation.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * Author: 吴天宇
 * Date: 2025/8/4 10:01
 * Description: 震动工具类（注意：需在清单文件中添加权限）
 */
public class VibrationUtils {
    private static final String TAG = "VibrationUtils";

    /**
     * 短震动（常用100ms）
     *
     * @param context 上下文
     */
    public static void shortVibrate(Context context) {
        if (context == null) {
            Log.e(TAG, "shortVibrate: Context cannot be null");
            return;
        }
        vibrate(context, 100);
    }

    /**
     * 长震动（常用500ms）
     *
     * @param context 上下文
     */
    public static void longVibrate(Context context) {
        if (context == null) {
            Log.e(TAG, "longVibrate: Context cannot be null");
            return;
        }
        vibrate(context, 500);
    }

    /**
     * 双击震动模式（短震-停顿-短震）
     *
     * @param context 上下文
     */
    public static void doubleClickVibrate(Context context) {
        if (context == null) {
            Log.e(TAG, "doubleClickVibrate: Context cannot be null");
            return;
        }
        // 模式：等待0ms -> 震动100ms -> 等待200ms -> 震动100ms
        long[] pattern = {0, 100, 200, 100};
        vibratePattern(context, pattern, -1);
    }

    /**
     * 单次震动
     *
     * @param context      上下文（不可为null）
     * @param milliseconds 震动时长(毫秒，需≥0)
     */
    public static void vibrate(Context context, long milliseconds) {
        if (context == null) {
            Log.e(TAG, "vibrate: Context cannot be null");
            return;
        }
        if (milliseconds < 0) {
            Log.e(TAG, "vibrate: Vibration duration cannot be negative");
            return;
        }

        Vibrator vibrator = getVibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(milliseconds);
            }
        } catch (Exception e) {
            Log.e(TAG, "vibrate: Exception occurred", e);
        }
    }

    /**
     * 波形震动
     *
     * @param context 上下文（不可为null）
     * @param pattern 震动模式数组（[等待,震动,等待,震动...]，不可为null且长度≥1）
     * @param repeat  重复次数（-1=不重复，0=从开头无限循环，正数=重复次数）
     */
    public static void vibratePattern(Context context, long[] pattern, int repeat) {
        if (context == null) {
            Log.e(TAG, "vibratePattern: Context cannot be null");
            return;
        }
        if (pattern == null || pattern.length == 0) {
            Log.e(TAG, "vibratePattern: Pattern cannot be null or empty");
            return;
        }

        int safeRepeat = Math.max(repeat, -1);
        if (safeRepeat >= pattern.length) {
            safeRepeat = -1;
        }

        Vibrator vibrator = getVibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 构建振幅数组（0表示不震动，DEFAULT_AMPLITUDE表示最大振幅）
                int[] amplitudes = new int[pattern.length];
                for (int i = 0; i < pattern.length; i++) {
                    // 偶数索引（0,2,4...）通常是等待时间，振幅设为0
                    amplitudes[i] = (i % 2 == 0) ? 0 : VibrationEffect.DEFAULT_AMPLITUDE;
                }
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, amplitudes, safeRepeat);
                vibrator.vibrate(effect);
            } else {
                // 旧版API直接使用pattern和repeat
                vibrator.vibrate(pattern, safeRepeat);
            }
        } catch (Exception e) {
            Log.e(TAG, "vibratePattern: Exception occurred", e);
        }
    }

    /**
     * 取消所有震动
     *
     * @param context 上下文（不可为null）
     */
    public static void cancelVibration(Context context) {
        if (context == null) {
            Log.e(TAG, "cancelVibration: Context cannot be null");
            return;
        }
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    /**
     * 检查设备是否支持震动
     *
     * @param context 上下文（不可为null）
     * @return 是否支持震动
     */
    public static boolean hasVibrator(Context context) {
        if (context == null) {
            Log.e(TAG, "hasVibrator: Context cannot be null");
            return false;
        }
        Vibrator vibrator = getVibrator(context);
        return vibrator != null && vibrator.hasVibrator();
    }

    /**
     * 获取系统震动服务
     *
     * @param context 上下文
     * @return 震动器实例（可能为null）
     */
    @SuppressWarnings("deprecation")
    private static Vibrator getVibrator(Context context) {
        if (context == null) {
            Log.e(TAG, "getVibrator: Context cannot be null");
            return null;
        }
        // Android 12+（API 31）使用VibratorManager获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            // 旧版本直接获取Vibrator服务
            return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }
}