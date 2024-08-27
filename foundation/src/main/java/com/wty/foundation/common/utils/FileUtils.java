package com.wty.foundation.common.utils;

import java.io.File;
import java.io.IOException;

import com.wty.foundation.common.init.AppContext;

import android.content.Context;
import android.os.Environment;

public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * 获取外部存储目录中的指定文件夹路径。
     *
     * @return 返回文件夹路径，如果获取失败则返回应用内部存储路径。
     */
    public static String getExternalFilesDir() {
        Context context = AppContext.getInstance().getContext();
        File file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String path = getPath(file);
        return StringUtils.isNullEmpty(path) ? context.getFilesDir().getPath() : path;
    }

    /**
     * 获取文件的规范化路径。
     *
     * @param file 文件对象。
     * @return 文件的规范化路径或null。
     */
    public static String getPath(File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath(); // 发生异常时回退到绝对路径
        }
    }

    /**
     * 创建多级目录。
     *
     * @param file 目标文件夹对象。
     * @return 如果成功创建或已经存在返回true，否则返回false。
     */
    public static boolean mkdirs(File file) {
        if (file == null || file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    /**
     * 创建多级目录。
     *
     * @param path 目标文件夹路径。
     * @return 如果成功创建或已经存在返回true，否则返回false。
     */
    public static boolean mkdirs(String path) {
        return mkdirs(new File(path));
    }

    /**
     * 检查文件是否存在。
     *
     * @param file 文件对象。
     * @return 如果文件存在返回true，否则返回false。
     */
    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    /**
     * 删除文件或者文件夹。
     *
     * @param file 要删除的文件或文件夹对象。
     * @return 如果删除成功返回true，否则返回false。
     */
    public static boolean delete(File file) {
        if (file == null) {
            return false;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!delete(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    /**
     * 重命名文件或文件夹。
     *
     * @param file 文件对象。
     * @param newName 新名称。
     * @return 如果重命名成功返回true，否则返回false。
     */
    public static boolean rename(File file, String newName) {
        if (file == null) {
            return false;
        }
        File newFile = new File(file.getParent(), newName);
        return file.renameTo(newFile);
    }

    /**
     * 获取文件大小。
     *
     * @param file 文件对象。
     * @return 文件大小，如果文件不存在返回-1。
     */
    public static long getSize(File file) {
        return file != null && file.exists() ? file.length() : -1;
    }

    /**
     * 判断是否是文件夹。
     *
     * @param file 文件对象。
     * @return 如果是文件夹返回true，否则返回false。
     */
    public static boolean isDirectory(File file) {
        return file != null && file.isDirectory();
    }

    /**
     * 判断是否是文件。
     *
     * @param file 文件对象。
     * @return 如果是文件返回true，否则返回false。
     */
    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }
}
