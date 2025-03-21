package com.wty.foundation.common.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MD5 {

    private static final Logger logger = Logger.getLogger(MD5.class.getName());

    /**
     * 计算输入字符串的MD5哈希值
     *
     * @param content 待计算MD5的字符串，若为null则视为空字符串
     * @return 输入字符串的MD5哈希值，若计算失败则返回空字符串
     */
    public static String md5(String content) {
        String processedContent = (content == null) ? "" : content;
        byte[] inputBytes;
        try {
            // 将处理后的字符串按UTF-8编码为字节数组
            inputBytes = encodeUtf8(processedContent);
        } catch (Exception e) {
            // 记录字符串编码异常信息，使用空字节数组继续执行
            logger.log(Level.SEVERE, "字符串编码异常，使用空字节数组继续执行", e);
            inputBytes = new byte[0];
        }
        byte[] hashBytes;
        try {
            // 获取MD5算法的消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // 计算输入字节数组的MD5哈希值
            hashBytes = digest.digest(inputBytes);
        } catch (NoSuchAlgorithmException e) {
            // 记录MD5算法不可用的异常信息
            logger.log(Level.SEVERE, "MD5算法不可用", e);
            return "";
        } catch (Exception e) {
            // 记录计算MD5时发生的未知异常信息
            logger.log(Level.SEVERE, "计算MD5时发生未知异常", e);
            return "";
        }
        // 将字节数组转换为十六进制字符串
        return bytesToHex(hashBytes);
    }

    /**
     * 将字符串按UTF-8编码为字节数组
     *
     * @param content 待编码的字符串
     * @return 编码后的字节数组，若系统不支持UTF-8编码则使用平台默认编码
     */
    private static byte[] encodeUtf8(String content) {
        try {
            // 按UTF-8编码字符串为字节数组
            return content.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // 记录系统不支持UTF-8编码的异常信息，使用平台默认编码
            logger.log(Level.SEVERE, "系统不支持UTF-8编码，使用平台默认编码", e);
            return content.getBytes();
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 待转换的字节数组
     * @return 转换后的十六进制字符串，若字节数组为null则返回空字符串
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        // 用于构建十六进制字符串的StringBuilder
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 确保字节为无符号值
            int value = b & 0xFF;
            if (value < 0x10) {
                // 若值小于16，在前面补零
                hex.append('0');
            }
            // 将无符号值转换为十六进制字符串并追加到StringBuilder中
            hex.append(Integer.toHexString(value));
        }
        // 返回构建好的十六进制字符串
        return hex.toString();
    }
}