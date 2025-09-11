package com.wty.foundation.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.wty.foundation.common.init.AppContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Author: 吴天宇
 * Date: 2025/2/20 16:31
 * Description: 文件工具类
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * 私有化构造函数,防止实例化
     */
    private FileUtils() {
    }

    /**
     * 获取应用的外部存储私有目录路径
     *
     * @return 外部存储私有目录路径，或回退后的内部存储路径
     */
    public static String getExternalFilesDir() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, returning fallback internal storage path");
            return Environment.getDataDirectory().getAbsolutePath();
        }

        // Android 10+ 不需要 WRITE_EXTERNAL_STORAGE 权限访问应用私有目录
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No permission for external storage, returning internal storage path");
            return context.getFilesDir().getPath();
        }

        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (externalDir != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String path = getPath(externalDir);
            if (path != null && !path.isEmpty()) {
                return path;
            }
        }

        Log.w(TAG, "External storage unavailable, fallback to internal storage");
        return context.getFilesDir().getPath();
    }

    /**
     * 获取文件的规范化路径，避免路径遍历问题
     *
     * @param file 目标文件，可为 null
     * @return 文件的规范化路径，若输入为 null 或出错则返回绝对路径或 null
     */
    public static String getPath(File file) {
        if (file == null) return null;
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to get canonical path", e);
            return file.getAbsolutePath();
        }
    }

    /**
     * 使用 UTF-8 编码和缓冲流将字符串内容写入文件
     * 自动创建不存在的父目录
     *
     * @param file    目标文件
     * @param content 要写入的字符串内容
     * @return 写入成功返回 true，否则返回 false
     */
    public static boolean writeToFile(File file, String content) {
        if (file == null || content == null) return false;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(content);
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * 使用 UTF-8 编码和缓冲流从文件读取字符串内容
     *
     * @param file 源文件
     * @return 文件内容的字符串，读取失败返回 null
     */
    public static String readFromFile(File file) {
        if (file == null || !file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file: " + file.getPath(), e);
            return null;
        }
    }

    /**
     * 创建目录，包括所有不存在的父目录
     *
     * @param file 目标目录文件
     * @return 创建成功或目录已存在返回 true，否则返回 false
     */
    public static boolean mkdirs(File file) {
        if (file == null) return false;
        if (file.exists()) return true;
        return file.mkdirs();
    }

    /**
     * 创建目录，包括所有不存在的父目录
     *
     * @param path 目标目录路径
     * @return 创建成功或目录已存在返回 true，否则返回 false
     */
    public static boolean mkdirs(String path) {
        if (path == null || path.isEmpty()) return false;
        return mkdirs(new File(path));
    }

    /**
     * 检查文件或目录是否存在
     *
     * @param file 目标文件
     * @return 存在返回 true，否则返回 false
     */
    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    /**
     * 递归删除文件或目录
     *
     * @param file 目标文件或目录
     * @return 删除成功返回 true，否则返回 false
     */
    public static boolean delete(File file) {
        if (file == null) return false;
        if (!exists(file)) return false;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!delete(child)) return false;
                }
            }
        }
        return file.delete();
    }

    /**
     * 重命名文件或目录
     *
     * @param file    目标文件或目录
     * @param newName 新名称
     * @return 重命名成功返回 true，否则返回 false
     */
    public static boolean rename(File file, String newName) {
        if (file == null || newName == null || newName.trim().isEmpty()) return false;
        File newFile = new File(file.getParent(), newName);
        return file.renameTo(newFile);
    }

    /**
     * 获取文件大小（字节）
     *
     * @param file 目标文件
     * @return 文件大小（字节），文件不存在返回 -1
     */
    public static long getSize(File file) {
        if (file == null || !exists(file)) return -1;
        return file.length();
    }

    /**
     * 检查路径是否为目录
     *
     * @param file 目标文件
     * @return 是目录返回 true，否则返回 false
     */
    public static boolean isDirectory(File file) {
        return file != null && file.isDirectory();
    }

    /**
     * 检查路径是否为文件
     *
     * @param file 目标文件
     * @return 是文件返回 true，否则返回 false
     */
    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }

    /**
     * 复制文件或目录（递归）
     * Android 8.0+ 使用 Files.copy，低版本使用流复制
     *
     * @param srcFile  源文件或目录
     * @param destFile 目标文件或目录
     * @return 复制成功返回 true，否则返回 false
     */
    public static boolean copy(File srcFile, File destFile) {
        if (srcFile == null || destFile == null || !exists(srcFile)) return false;
        try {
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(Paths.get(srcFile.getAbsolutePath()), Paths.get(destFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            } else {
                try (InputStream in = new FileInputStream(srcFile); OutputStream out = new FileOutputStream(destFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file from " + srcFile.getPath() + " to " + destFile.getPath(), e);
            return false;
        }
    }

    /**
     * 移动文件或目录
     * 先尝试直接重命名，失败则使用复制+删除方式
     *
     * @param srcFile  源文件或目录
     * @param destFile 目标文件或目录
     * @return 移动成功返回 true，否则返回 false
     */
    public static boolean move(File srcFile, File destFile) {
        if (srcFile == null || destFile == null || !exists(srcFile)) return false;
        if (srcFile.renameTo(destFile)) return true;
        return copy(srcFile, destFile) && delete(srcFile);
    }
}