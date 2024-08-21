package com.wty.foundation.common.utils;

import java.io.File;
import java.io.IOException;

import com.wty.foundation.common.init.AppContext;

import android.content.Context;
import android.os.Environment;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static String getExternalFilesDir() {
        Context context = AppContext.getInstance().getContext();
        File file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String path = getPath(file);
        return StringUtils.isNullEmpty(path) ? "/data/data/" + context.getPackageName() + "" : path;
    }

    public static String getPath(File file) {
        try {
            return file == null ? null : file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean mkdirs(File file) {
        if (file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    public static boolean mkdirs(String path) {
        return mkdirs(new File(path));
    }
}
