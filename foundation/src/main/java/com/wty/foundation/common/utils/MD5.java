package com.wty.foundation.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    /**
     * 使用MD5算法生成字符串的散列值。
     *
     * @param content 需要进行散列的字符串
     * @return 返回一个表示MD5哈希值的十六进制字符串
     */
    public static String md5(String content) {
        // 将输入的字符串转换为字节数组
        byte[] hash;
        try {
            // 获取MD5消息摘要算法实例
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // 计算输入内容的摘要
            hash = digest.digest(encodeUtf8(content));
        } catch (NoSuchAlgorithmException e) {
            // 如果没有找到MD5算法，则抛出运行时异常
            throw new RuntimeException("无法获取MD5算法实例", e);
        }

        // 将字节数据转换为十六进制字符串
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    /**
     * 将字符串编码为UTF-8格式的字节数组。
     *
     * @param content 要编码的字符串
     * @return UTF-8格式的字节数组
     */
    private static byte[] encodeUtf8(String content) {
        try {
            // 使用UTF-8编码将字符串转化为字节数组
            return content.getBytes("UTF-8");
        } catch (Exception e) {
            // 如果出现异常，则使用默认平台编码，并记录错误信息
            System.err.println("UTF-8编码转换失败: " + e.getMessage());
            return content.getBytes();
        }
    }
}
