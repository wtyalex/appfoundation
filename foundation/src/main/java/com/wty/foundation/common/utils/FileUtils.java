package com.wty.foundation.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.wty.foundation.common.init.AppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件工具类
 * 功能包括：路径获取、文件内容读写、文件操作（删除、复制、移动等）、目录操作等。
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    // 私有构造函数，禁止实例化
    private FileUtils() {
    }

    /**
     * 获取外部存储目录中的指定文件夹路径
     *
     * @return 返回文件夹路径，如果获取失败则返回应用内部存储路径
     */
    public static String getExternalFilesDir() {
        Context context = AppContext.getInstance().getContext();
        if (context == null) {
            Log.e(TAG, "Context is null, unable to get external files directory.");
            return context.getFilesDir().getPath(); // 返回内部存储路径
        }

        // 检查是否有写入外部存储的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permission not granted for writing to external storage. Returning internal storage path.");
            return context.getFilesDir().getPath(); // 如果没有权限，则返回内部存储路径
        }

        // 获取外部存储目录
        File file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        // 检查外部存储是否可用
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String path = getPath(file);
            if (StringUtils.isNullEmpty(path)) {
                Log.w(TAG, "External files directory path is null or empty, returning internal storage path.");
                return context.getFilesDir().getPath(); // 如果外部路径无效，则返回内部路径
            } else {
                return path;
            }
        } else {
            Log.w(TAG, "External storage is not mounted, returning internal storage path.");
            return context.getFilesDir().getPath(); // 如果外部存储不可用，则返回内部路径
        }
    }

    /**
     * 获取文件的规范化路径
     *
     * @param file 文件对象
     * @return 文件的规范化路径或null
     */
    public static String getPath(File file) {
        if (file == null) {
            Log.e(TAG, "getPath: file is null");
            return null;
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to get canonical path for file", e);
            return file.getAbsolutePath(); // 发生异常时回退到绝对路径
        }
    }

    /**
     * 写入内容到文件
     *
     * @param file    文件对象
     * @param content 写入的内容
     * @return 是否写入成功
     */
    public static boolean writeToFile(File file, String content) {
        if (file == null || content == null) {
            Log.e(TAG, "writeToFile: invalid parameters");
            return false;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write content to file: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * 从文件读取内容
     *
     * @param file 文件对象
     * @return 读取的内容
     */
    public static String readFromFile(File file) {
        if (file == null || !file.exists()) {
            Log.e(TAG, "readFromFile: file does not exist");
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read content from file: " + file.getPath(), e);
            return null;
        }
    }

    /**
     * 创建多级目录
     *
     * @param file 目标文件夹对象
     * @return 如果成功创建或已经存在返回true，否则返回false
     */
    public static boolean mkdirs(File file) {
        if (file == null) {
            Log.e(TAG, "mkdirs: file is null");
            return false;
        }
        if (file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    /**
     * 创建多级目录
     *
     * @param path 目标文件夹路径
     * @return 如果成功创建或已经存在返回true，否则返回false
     */
    public static boolean mkdirs(String path) {
        if (path == null || path.isEmpty()) {
            Log.e(TAG, "mkdirs: path is null or empty");
            return false;
        }
        return mkdirs(new File(path));
    }

    /**
     * 检查文件是否存在
     *
     * @param file 文件对象
     * @return 如果文件存在返回true，否则返回false
     */
    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    /**
     * 删除文件或者文件夹
     *
     * @param file 要删除的文件或文件夹对象
     * @return 如果删除成功返回true，否则返回false
     */
    public static boolean delete(File file) {
        if (file == null) {
            Log.e(TAG, "delete: file is null");
            return false;
        }
        if (!exists(file)) {
            Log.w(TAG, "delete: file does not exist");
            return false;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!delete(child)) {
                        Log.e(TAG, "Failed to delete child file: " + child.getPath());
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    /**
     * 重命名文件或文件夹
     *
     * @param file    文件对象
     * @param newName 新名称
     * @return 如果重命名成功返回true，否则返回false
     */
    public static boolean rename(File file, String newName) {
        if (file == null || newName == null || newName.trim().isEmpty()) {
            Log.e(TAG, "rename: invalid parameters");
            return false;
        }
        File newFile = new File(file.getParent(), newName);
        return file.renameTo(newFile);
    }

    /**
     * 获取文件大小
     *
     * @param file 文件对象
     * @return 文件大小，如果文件不存在返回-1
     */
    public static long getSize(File file) {
        if (file == null || !exists(file)) {
            Log.e(TAG, "getSize: file does not exist");
            return -1;
        }
        return file.length();
    }

    /**
     * 判断是否是文件夹
     *
     * @param file 文件对象
     * @return 如果是文件夹返回true，否则返回false
     */
    public static boolean isDirectory(File file) {
        return file != null && file.isDirectory();
    }

    /**
     * 判断是否是文件
     *
     * @param file 文件对象
     * @return 如果是文件返回true，否则返回false
     */
    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }

    /**
     * 复制文件
     *
     * @param srcFile  源文件对象
     * @param destFile 目标文件对象
     * @return 如果复制成功返回true，否则返回false
     */
    public static boolean copy(File srcFile, File destFile) {
        if (srcFile == null || destFile == null) {
            Log.e(TAG, "copy: invalid parameters");
            return false;
        }
        if (!exists(srcFile)) {
            Log.e(TAG, "copy: source file does not exist");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 使用 Java NIO 进行复制，适用于 Android 8.0 及以上
                Files.copy(Paths.get(srcFile.getAbsolutePath()), Paths.get(destFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy file from " + srcFile.getPath() + " to " + destFile.getPath(), e);
                return false;
            }
        } else {
            // 对于较低版本的 Android，使用传统的输入输出流进行复制
            try (InputStream in = new FileInputStream(srcFile); OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy file from " + srcFile.getPath() + " to " + destFile.getPath(), e);
                return false;
            }
        }
    }

    /**
     * 移动文件
     *
     * @param srcFile  源文件
     * @param destFile 目标文件
     * @return 如果移动成功返回true，否则返回false
     */
    public static boolean move(File srcFile, File destFile) {
        if (srcFile == null || destFile == null) {
            Log.e(TAG, "move: invalid parameters");
            return false;
        }
        if (!exists(srcFile)) {
            Log.e(TAG, "move: source file does not exist");
            return false;
        }
        // 先尝试重命名，如果不行再复制后删除源文件
        if (srcFile.renameTo(destFile)) {
            return true;
        } else {
            if (copy(srcFile, destFile) && delete(srcFile)) {
                return true;
            } else {
                Log.e(TAG, "Failed to move file from " + srcFile.getPath() + " to " + destFile.getPath());
                return false;
            }
        }
    }
}