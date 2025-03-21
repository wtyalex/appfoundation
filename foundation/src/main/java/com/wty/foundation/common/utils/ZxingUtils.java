package com.wty.foundation.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ZxingUtils {
    private static final String TAG = "ZxingUtils";
    // 默认字符集
    private static final String DEFAULT_CHARSET = "UTF-8";
    // 默认边距
    private static final int DEFAULT_MARGIN = 1;
    // 默认纠错级别
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.H;
    // 主线程处理器
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    // 最大尺寸
    private static final int MAX_SIZE = 4096;
    // 默认配置
    private static final Config DEFAULT_CONFIG = new Config();

    /**
     * 生成结果的回调接口
     */
    public interface Callback {
        /**
         * 生成成功时调用
         *
         * @param bitmap 生成的位图，可能为空
         */
        void onSuccess(@Nullable Bitmap bitmap);

        /**
         * 生成失败时调用
         *
         * @param errorMsg 错误信息
         */
        void onFailure(@NonNull String errorMsg);
    }

    /**
     * 二维码生成配置类
     */
    public static class Config {
        // 前景色
        @ColorInt
        int fgColor = Color.BLACK;
        // 背景色
        @ColorInt
        int bgColor = Color.WHITE;
        // 边距
        int margin = DEFAULT_MARGIN;
        // 纠错级别
        ErrorCorrectionLevel errorCorrection = DEFAULT_ERROR_CORRECTION;
        // 二维码中心的 logo，可能为空
        @Nullable
        Bitmap logo;
        // logo 尺寸比例
        float logoSizeRatio = 0.2f;
        // 是否显示 logo 边框
        boolean logoBorder = true;
        // logo 边框颜色
        @ColorInt
        int logoBorderColor = Color.WHITE;
        // 条码格式
        BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;

        /**
         * 设置前景色
         *
         * @param color 颜色值
         * @return 配置对象本身
         */
        public Config setFgColor(@ColorInt int color) {
            this.fgColor = color;
            return this;
        }

        /**
         * 设置背景色
         *
         * @param color 颜色值
         * @return 配置对象本身
         */
        public Config setBgColor(@ColorInt int color) {
            this.bgColor = color;
            return this;
        }

        /**
         * 设置边距
         *
         * @param margin 边距值，最小为 0
         * @return 配置对象本身
         */
        public Config setMargin(int margin) {
            this.margin = Math.max(0, margin);
            return this;
        }

        /**
         * 设置纠错级别
         *
         * @param level 纠错级别
         * @return 配置对象本身
         */
        public Config setErrorCorrection(ErrorCorrectionLevel level) {
            this.errorCorrection = level;
            return this;
        }

        /**
         * 设置 logo
         *
         * @param logo 位图 logo，可能为空
         * @return 配置对象本身
         */
        public Config setLogo(@Nullable Bitmap logo) {
            this.logo = logo;
            return this;
        }

        /**
         * 设置 logo 尺寸比例
         *
         * @param ratio 比例值，范围在 0.1f 到 0.3f 之间
         * @return 配置对象本身
         */
        public Config setLogoSizeRatio(float ratio) {
            this.logoSizeRatio = clamp(ratio, 0.1f, 0.3f);
            return this;
        }

        /**
         * 设置是否显示 logo 边框
         *
         * @param showBorder 是否显示边框
         * @return 配置对象本身
         */
        public Config setLogoBorder(boolean showBorder) {
            this.logoBorder = showBorder;
            return this;
        }

        /**
         * 设置 logo 边框颜色
         *
         * @param color 颜色值
         * @return 配置对象本身
         */
        public Config setLogoBorderColor(@ColorInt int color) {
            this.logoBorderColor = color;
            return this;
        }

        /**
         * 设置条码格式
         *
         * @param format 条码格式
         * @return 配置对象本身
         */
        public Config setBarcodeFormat(BarcodeFormat format) {
            this.barcodeFormat = format;
            return this;
        }
    }

    /**
     * 异步生成条码
     *
     * @param content  条码内容
     * @param size     条码尺寸
     * @param config   生成配置，可能为空
     * @param callback 生成结果回调
     */
    public static void generateAsync(@NonNull String content, int size, @Nullable Config config, @NonNull Callback callback) {
        SafeAsyncTask task = new SafeAsyncTask(content, size, config, callback);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * 同步生成条码
     *
     * @param content 条码内容，可能为空
     * @param size    条码尺寸
     * @param config  生成配置，可能为空
     * @return 生成的位图，可能为空
     */
    @Nullable
    public static Bitmap generateSync(@Nullable String content, int size, @Nullable Config config) {
        if (!validateContent(content) || !validateSize(size)) return null;

        try {
            Config finalConfig = mergeConfig(config);
            Map<EncodeHintType, Object> hints = prepareHints(finalConfig);

            BitMatrix matrix = new MultiFormatWriter().encode(content, finalConfig.barcodeFormat, size, size, hints);

            Bitmap baseBitmap = renderToBitmap(matrix, finalConfig);
            if (baseBitmap == null) return null;

            return finalConfig.barcodeFormat == BarcodeFormat.QR_CODE ? addLogo(baseBitmap, finalConfig) : baseBitmap;
        } catch (WriterException | IllegalArgumentException e) {
            Log.e(TAG, "Generation failed: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            handleOOMError("generateSync");
        }
        return null;
    }

    /**
     * 将位图保存到文件
     *
     * @param context    上下文对象
     * @param bitmap     位图，可能为空
     * @param outputFile 输出文件
     * @param quality    压缩质量
     * @return 是否保存成功
     */
    public static boolean saveToFile(@NonNull Context context, @Nullable Bitmap bitmap, @NonNull File outputFile, int quality) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "Invalid bitmap");
            return false;
        }

        if (requireStoragePermission(outputFile) && !checkStoragePermission(context)) {
            Log.e(TAG, "Storage permission denied");
            return false;
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.e(TAG, "Failed to create directories: " + parent);
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            Bitmap.CompressFormat format = guessImageFormat(outputFile.getName());
            if (!bitmap.compress(format, quality, fos)) {
                Log.e(TAG, "Bitmap compression failed");
                return false;
            }
            fos.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "File save failed: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * 验证条码内容是否有效
     *
     * @param content 条码内容，可能为空
     * @return 是否有效
     */
    private static boolean validateContent(@Nullable String content) {
        if (content == null || content.trim().isEmpty()) {
            Log.w(TAG, "Content cannot be empty");
            return false;
        }
        return true;
    }

    /**
     * 验证条码尺寸是否有效
     *
     * @param size 条码尺寸
     * @return 是否有效
     */
    private static boolean validateSize(int size) {
        if (size <= 0 || size > MAX_SIZE) {
            Log.w(TAG, "Invalid size: " + size + ", valid range: [1, " + MAX_SIZE + "]");
            return false;
        }
        return true;
    }

    /**
     * 合并配置
     *
     * @param input 输入配置，可能为空
     * @return 合并后的配置
     */
    @NonNull
    private static Config mergeConfig(@Nullable Config input) {
        Config config = new Config();
        if (input == null) return config;

        config.fgColor = input.fgColor;
        config.bgColor = input.bgColor;
        config.margin = input.margin > 0 ? input.margin : DEFAULT_MARGIN;
        config.errorCorrection = input.errorCorrection != null ? input.errorCorrection : DEFAULT_ERROR_CORRECTION;
        config.logo = input.logo != null && !input.logo.isRecycled() ? input.logo : null;
        config.logoSizeRatio = clamp(input.logoSizeRatio, 0.1f, 0.3f);
        config.logoBorder = input.logoBorder;
        config.logoBorderColor = input.logoBorderColor;
        config.barcodeFormat = input.barcodeFormat != null ? input.barcodeFormat : BarcodeFormat.QR_CODE;
        return config;
    }

    /**
     * 限制值在指定范围内
     *
     * @param value 要限制的值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 准备编码提示信息
     *
     * @param config 配置对象
     * @return 编码提示信息
     */
    @NonNull
    private static Map<EncodeHintType, Object> prepareHints(@NonNull Config config) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
        hints.put(EncodeHintType.MARGIN, config.margin);

        if (config.barcodeFormat == BarcodeFormat.QR_CODE) {
            hints.put(EncodeHintType.ERROR_CORRECTION, config.errorCorrection);
        }
        return Collections.unmodifiableMap(hints);
    }

    /**
     * 将 BitMatrix 渲染为位图
     *
     * @param matrix BitMatrix 对象
     * @param config 配置对象
     * @return 生成的位图，可能为空
     */
    @Nullable
    private static Bitmap renderToBitmap(@NonNull BitMatrix matrix, @NonNull Config config) {
        try {
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? config.fgColor : config.bgColor;
                }
            }

            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            handleOOMError("renderToBitmap");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid bitmap parameters: " + e.getMessage());
        }
        return null;
    }

    /**
     * 为二维码添加 logo
     *
     * @param qrBitmap 二维码位图
     * @param config   配置对象
     * @return 添加 logo 后的位图，可能未改变
     */
    @Nullable
    private static Bitmap addLogo(@NonNull Bitmap qrBitmap, @NonNull Config config) {
        if (config.logo == null || config.logo.isRecycled()) return qrBitmap;

        try {
            Canvas canvas = new Canvas(qrBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            int qrSize = qrBitmap.getWidth();
            int logoSize = (int) (qrSize * config.logoSizeRatio);
            Bitmap scaledLogo = Bitmap.createScaledBitmap(config.logo, logoSize, logoSize, true);

            if (config.logoBorder) {
                drawLogoBorder(canvas, qrSize, logoSize, config.logoBorderColor, paint);
            }

            RectF destRect = new RectF((qrSize - logoSize) / 2f, (qrSize - logoSize) / 2f, (qrSize + logoSize) / 2f, (qrSize + logoSize) / 2f);
            canvas.drawBitmap(scaledLogo, null, destRect, paint);
            return qrBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Logo addition failed: " + e.getMessage());
            return qrBitmap;
        }
    }

    /**
     * 绘制 logo 边框
     *
     * @param canvas      画布对象
     * @param qrSize      二维码尺寸
     * @param logoSize    logo 尺寸
     * @param borderColor 边框颜色
     * @param paint       画笔对象
     */
    private static void drawLogoBorder(Canvas canvas, int qrSize, int logoSize, int borderColor, Paint paint) {
        paint.setColor(borderColor);
        float borderWidth = logoSize * 0.1f;
        float radius = logoSize / 2f + borderWidth;
        canvas.drawCircle(qrSize / 2f, qrSize / 2f, radius, paint);
    }

    /**
     * 判断是否需要存储权限
     *
     * @param outputFile 输出文件
     * @return 是否需要权限
     */
    private static boolean requireStoragePermission(File outputFile) {
        return isExternalStoragePath(outputFile.getAbsolutePath());
    }

    /**
     * 判断路径是否为外部存储路径
     *
     * @param path 路径
     * @return 是否为外部存储路径
     */
    private static boolean isExternalStoragePath(String path) {
        return path.startsWith("/storage/") || path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    /**
     * 检查存储权限
     *
     * @param context 上下文对象
     * @return 是否有存储权限
     */
    private static boolean checkStoragePermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager() || hasPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) && hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    /**
     * 检查是否有指定权限
     *
     * @param context    上下文对象
     * @param permission 权限名称
     * @return 是否有该权限
     */
    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 猜测图片格式
     *
     * @param fileName 文件名
     * @return 图片压缩格式
     */
    @NonNull
    private static Bitmap.CompressFormat guessImageFormat(@NonNull String fileName) {
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";

        switch (ext) {
            case "jpg":
            case "jpeg":
                return Bitmap.CompressFormat.JPEG;
            case "webp":
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;
            default:
                return Bitmap.CompressFormat.PNG;
        }
    }

    /**
     * 处理内存溢出错误
     *
     * @param methodName 发生错误的方法名
     */
    private static void handleOOMError(String methodName) {
        Log.e(TAG, "OOM in " + methodName + ", suggest reducing image size");
        System.gc();
    }

    /**
     * 安全异步任务类
     */
    private static class SafeAsyncTask extends AsyncTask<Void, Void, Bitmap> {
        // 回调弱引用
        private final WeakReference<Callback> callbackRef;
        // 条码内容
        private final String content;
        // 条码尺寸
        private final int size;
        // 配置对象
        private final Config config;
        // 错误信息
        private String errorMsg;

        /**
         * 构造函数
         *
         * @param content  条码内容
         * @param size     条码尺寸
         * @param config   配置对象，可能为空
         * @param callback 回调对象
         */
        SafeAsyncTask(@NonNull String content, int size, @Nullable Config config, @NonNull Callback callback) {
            this.content = content;
            this.size = Math.min(size, MAX_SIZE);
            this.config = config != null ? config : new Config();
            this.callbackRef = new WeakReference<>(callback);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return generateSync(content, size, config);
        }

        @Override
        protected void onPostExecute(@Nullable Bitmap result) {
            Callback callback = callbackRef.get();
            if (callback == null) return;

            if (result != null) {
                postSuccess(callback, result);
            } else {
                postFailure(callback, errorMsg != null ? errorMsg : "Generation failed");
            }
        }

        /**
         * 发布成功结果
         *
         * @param callback 回调对象
         * @param bitmap   生成的位图，可能为空
         */
        private void postSuccess(@NonNull Callback callback, @Nullable Bitmap bitmap) {
            if (isMainThread()) {
                callback.onSuccess(bitmap);
            } else {
                MAIN_HANDLER.post(() -> callback.onSuccess(bitmap));
            }
        }

        /**
         * 发布失败结果
         *
         * @param callback 回调对象
         * @param msg      错误信息
         */
        private void postFailure(@NonNull Callback callback, @NonNull String msg) {
            if (isMainThread()) {
                callback.onFailure(msg);
            } else {
                MAIN_HANDLER.post(() -> callback.onFailure(msg));
            }
        }

        /**
         * 判断是否为主线程
         *
         * @return 是否为主线程
         */
        private boolean isMainThread() {
            return Looper.myLooper() == Looper.getMainLooper();
        }
    }
}