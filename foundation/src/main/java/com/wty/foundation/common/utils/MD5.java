package com.wty.foundation.common.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具类
 * 提供字符串和字节数组的MD5哈希计算功能，返回32位小写十六进制字符串
 */
public class MD5 {
    private static final String TAG = "MD5";
    private static final String UTF8 = "UTF-8";
    private static final String MD5_ALGORITHM = "MD5";

    /**
     * 计算字符串的MD5哈希值
     *
     * @param content 待计算字符串（null将被视为空字符串处理）
     * @return 32位小写MD5哈希值，计算失败时返回空字符串
     */
    public static String md5(String content) {
        // 安全处理空指针，将null转换为空字符串
        String safeContent = content == null ? "" : content;

        // 将字符串转换为字节数组（优先使用UTF-8编码）
        byte[] inputBytes;
        try {
            inputBytes = safeContent.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "不支持UTF-8编码，将使用平台默认编码", e);
            inputBytes = safeContent.getBytes();
        }

        // 复用字节数组的MD5计算逻辑
        return calculateMd5(inputBytes);
    }

    /**
     * 计算字节数组的MD5值
     *
     * @param data 待计算的字节数组（null将返回空字符串）
     * @return 32位小写MD5字符串，计算失败时返回空字符串
     */
    public static String calculateMd5(byte[] data) {
        // 处理空输入
        if (data == null) {
            Log.w(TAG, "calculateMd5: 输入字节数组为null，返回空字符串");
            return "";
        }

        try {
            // 获取MD5消息摘要实例
            MessageDigest digest = MessageDigest.getInstance(MD5_ALGORITHM);
            // 计算哈希值
            byte[] hashBytes = digest.digest(data);
            // 转换为十六进制字符串
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5算法不可用", e);
        }
        return "";
    }

    /**
     * 将字节数组转换为32位小写十六进制字符串
     * 确保每个字节转换为两位十六进制数（不足补0）
     *
     * @param bytes 原始字节数组（null将返回空字符串）
     * @return 转换后的32位小写十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            // 将字节转换为无符号整数（0~255）
            int unsignedByte = b & 0xFF;
            // 转换为十六进制字符串
            String hexStr = Integer.toHexString(unsignedByte);
            // 补0确保两位长度
            if (hexStr.length() == 1) {
                hexBuilder.append('0');
            }
            hexBuilder.append(hexStr);
        }
        return hexBuilder.toString();
    }
}