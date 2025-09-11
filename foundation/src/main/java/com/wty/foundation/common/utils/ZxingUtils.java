package com.wty.foundation.common.utils;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
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
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: 吴天宇
 * Date: 2025/2/20 16:31
 * Description: 二维码/条形码生成工具类
 */
public class ZxingUtils {
    private static final String TAG = "ZxingUtils";
    /**
     * 生成图片最大尺寸
     */
    private static final int MAX_SIZE = 4096;
    /**
     * 默认字符编码
     */
    private static final String DEFAULT_CHARSET = "UTF-8";
    /**
     * 默认边距
     */
    private static final int DEFAULT_MARGIN = 1;
    /**
     * 默认容错级别
     */
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.H;
    /**
     * 线程池用于异步任务
     */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * 生成结果回调接口
     */
    public interface Callback {
        /**
         * 生成成功回调
         *
         * @param bitmap 生成的位图，可能为null
         */
        void onSuccess(@Nullable Bitmap bitmap);

        /**
         * 生成失败回调
         *
         * @param errorMsg 错误信息
         */
        void onFailure(@NonNull String errorMsg);
    }

    /**
     * 生成配置类
     * 用于自定义条码样式、Logo、水印等参数
     */
    public static class Config {
        /**
         * 前景色（条码颜色）
         */
        @ColorInt
        public int fgColor = Color.BLACK;
        /**
         * 背景色
         */
        @ColorInt
        public int bgColor = Color.WHITE;
        /**
         * 边距
         */
        public int margin = DEFAULT_MARGIN;
        /**
         * 容错级别（主要用于QR_CODE）
         */
        public ErrorCorrectionLevel errorCorrection = DEFAULT_ERROR_CORRECTION;
        /**
         * Logo位图
         */
        @Nullable
        public Bitmap logo;
        /**
         * Logo大小相对于二维码的比例
         */
        public float logoSizeRatio = 0.2f;
        /**
         * 是否显示Logo边框
         */
        public boolean logoBorder = true;
        /**
         * Logo边框颜色
         */
        @ColorInt
        public int logoBorderColor = Color.WHITE;
        /**
         * Logo是否圆形显示
         */
        public boolean logoRound = true;
        /**
         * 是否启用前景色渐变
         */
        public boolean fgGradient = false;
        /**
         * 渐变起始颜色
         */
        @ColorInt
        public int gradientStartColor = Color.BLACK;
        /**
         * 渐变结束颜色
         */
        @ColorInt
        public int gradientEndColor = Color.BLACK;
        /**
         * 条码格式（QR_CODE、CODE_128等）
         */
        public BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;

        /**
         * 水印文字
         */
        @Nullable
        public String watermarkText;
        /**
         * 水印颜色
         */
        @ColorInt
        public int watermarkColor = Color.BLACK;
        /**
         * 水印字体大小（像素）
         */
        public float watermarkSize = 20f;
        /**
         * 水印X位置（百分比）
         */
        public float watermarkX = 0.5f;
        /**
         * 水印Y位置（百分比）
         */
        public float watermarkY = 0.95f;

        /**
         * 设置前景色
         *
         * @param color 颜色值
         * @return 当前Config实例
         */
        public Config setFgColor(@ColorInt int color) {
            fgColor = color;
            return this;
        }

        /**
         * 设置背景色
         *
         * @param color 颜色值
         * @return 当前Config实例
         */
        public Config setBgColor(@ColorInt int color) {
            bgColor = color;
            return this;
        }

        /**
         * 设置边距
         *
         * @param m 边距值（≥0）
         * @return 当前Config实例
         */
        public Config setMargin(int m) {
            margin = Math.max(0, m);
            return this;
        }

        /**
         * 设置容错级别
         *
         * @param level 容错级别
         * @return 当前Config实例
         */
        public Config setErrorCorrection(ErrorCorrectionLevel level) {
            errorCorrection = level;
            return this;
        }

        /**
         * 设置Logo
         *
         * @param b Logo位图
         * @return 当前Config实例
         */
        public Config setLogo(@Nullable Bitmap b) {
            logo = b;
            return this;
        }

        /**
         * 设置Logo大小比例
         *
         * @param r 比例值（0.1-0.3）
         * @return 当前Config实例
         */
        public Config setLogoSizeRatio(float r) {
            logoSizeRatio = clamp(r, 0.1f, 0.3f);
            return this;
        }

        /**
         * 设置是否显示Logo边框
         *
         * @param show 是否显示
         * @return 当前Config实例
         */
        public Config setLogoBorder(boolean show) {
            logoBorder = show;
            return this;
        }

        /**
         * 设置Logo边框颜色
         *
         * @param c 颜色值
         * @return 当前Config实例
         */
        public Config setLogoBorderColor(@ColorInt int c) {
            logoBorderColor = c;
            return this;
        }

        /**
         * 设置Logo是否圆形显示
         *
         * @param round 是否圆形
         * @return 当前Config实例
         */
        public Config setLogoRound(boolean round) {
            logoRound = round;
            return this;
        }

        /**
         * 设置前景色渐变
         *
         * @param start 起始颜色
         * @param end   结束颜色
         * @return 当前Config实例
         */
        public Config setFgGradient(int start, int end) {
            fgGradient = true;
            gradientStartColor = start;
            gradientEndColor = end;
            return this;
        }

        /**
         * 设置条码格式
         *
         * @param f 条码格式
         * @return 当前Config实例
         */
        public Config setBarcodeFormat(BarcodeFormat f) {
            barcodeFormat = f;
            return this;
        }

        /**
         * 设置水印
         *
         * @param text   水印文字
         * @param color  水印颜色
         * @param size   水印字体大小
         * @param xRatio X位置比例
         * @param yRatio Y位置比例
         * @return 当前Config实例
         */
        public Config setWatermark(String text, int color, float size, float xRatio, float yRatio) {
            watermarkText = text;
            watermarkColor = color;
            watermarkSize = size;
            watermarkX = xRatio;
            watermarkY = yRatio;
            return this;
        }
    }

    /**
     * 限制数值在指定范围内
     *
     * @param value 输入值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 异步生成条码
     *
     * @param content  编码内容
     * @param size     图片尺寸（宽高相等）
     * @param config   生成配置
     * @param callback 生成结果回调
     */
    public static void generateAsync(@NonNull String content, int size, @Nullable Config config, @NonNull Callback callback) {
        EXECUTOR.execute(() -> {
            Bitmap bitmap = generateSync(content, size, config);
            if (bitmap != null) postSuccess(callback, bitmap);
            else postFailure(callback, "QR/Barcode generation failed");
        });
    }

    /**
     * 同步生成条码
     *
     * @param content 编码内容
     * @param size    图片尺寸（宽高相等）
     * @param config  生成配置
     * @return 生成的位图，失败返回null
     */
    @Nullable
    public static Bitmap generateSync(@Nullable String content, int size, @Nullable Config config) {
        if (!validateContent(content) || !validateSize(size)) return null;
        try {
            Config cfg = mergeConfig(config);
            Map<EncodeHintType, Object> hints = prepareHints(cfg);
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(content, cfg.barcodeFormat, size, size, hints);

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap base = encoder.createBitmap(matrix);
            base = applyColor(base, cfg);
            base = addLogo(base, cfg);
            base = addWatermark(base, cfg);

            return base;
        } catch (WriterException e) {
            Log.e(TAG, "WriterException: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            handleOOMError("generateSync");
        }
        return null;
    }

    /**
     * 验证内容有效性
     *
     * @param c 编码内容
     * @return 内容有效返回true
     */
    private static boolean validateContent(@Nullable String c) {
        return c != null && !c.trim().isEmpty();
    }

    /**
     * 验证尺寸有效性
     *
     * @param size 图片尺寸
     * @return 尺寸有效返回true
     */
    private static boolean validateSize(int size) {
        return size > 0 && size <= MAX_SIZE;
    }

    /**
     * 合并配置参数
     *
     * @param input 输入配置
     * @return 合并后的配置（不会为null）
     */
    @NonNull
    private static Config mergeConfig(@Nullable Config input) {
        if (input == null) return new Config();
        Config cfg = new Config();
        cfg.fgColor = input.fgColor;
        cfg.bgColor = input.bgColor;
        cfg.margin = input.margin > 0 ? input.margin : DEFAULT_MARGIN;
        cfg.errorCorrection = input.errorCorrection != null ? input.errorCorrection : DEFAULT_ERROR_CORRECTION;
        cfg.logo = input.logo != null && !input.logo.isRecycled() ? input.logo : null;
        cfg.logoSizeRatio = clamp(input.logoSizeRatio, 0.1f, 0.3f);
        cfg.logoBorder = input.logoBorder;
        cfg.logoBorderColor = input.logoBorderColor;
        cfg.barcodeFormat = input.barcodeFormat != null ? input.barcodeFormat : BarcodeFormat.QR_CODE;
        cfg.logoRound = input.logoRound;
        cfg.fgGradient = input.fgGradient;
        cfg.gradientStartColor = input.gradientStartColor;
        cfg.gradientEndColor = input.gradientEndColor;
        cfg.watermarkText = input.watermarkText;
        cfg.watermarkColor = input.watermarkColor;
        cfg.watermarkSize = input.watermarkSize;
        cfg.watermarkX = input.watermarkX;
        cfg.watermarkY = input.watermarkY;
        return cfg;
    }

    /**
     * 准备编码参数
     *
     * @param cfg 生成配置
     * @return 编码参数映射
     */
    @NonNull
    private static Map<EncodeHintType, Object> prepareHints(@NonNull Config cfg) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
        hints.put(EncodeHintType.MARGIN, cfg.margin);
        if (cfg.barcodeFormat == BarcodeFormat.QR_CODE)
            hints.put(EncodeHintType.ERROR_CORRECTION, cfg.errorCorrection);
        return hints;
    }

    /**
     * 应用颜色配置
     *
     * @param bmp 原始位图
     * @param cfg 生成配置
     * @return 应用颜色后的位图
     */
    private static Bitmap applyColor(@NonNull Bitmap bmp, @NonNull Config cfg) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        Bitmap out = bmp.copy(Bitmap.Config.ARGB_8888, true);
        int[] pixels = new int[w * h];
        out.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.BLACK) {
                if (cfg.fgGradient) {
                    int y = i / w;
                    float ratio = (float) y / h;
                    pixels[i] = blend(cfg.gradientStartColor, cfg.gradientEndColor, ratio);
                } else {
                    pixels[i] = cfg.fgColor;
                }
            } else {
                pixels[i] = cfg.bgColor;
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    /**
     * 颜色混合计算
     *
     * @param start 起始颜色
     * @param end   结束颜色
     * @param ratio 混合比例
     * @return 混合后的颜色
     */
    private static int blend(int start, int end, float ratio) {
        int r = (int) (Color.red(start) * (1 - ratio) + Color.red(end) * ratio);
        int g = (int) (Color.green(start) * (1 - ratio) + Color.green(end) * ratio);
        int b = (int) (Color.blue(start) * (1 - ratio) + Color.blue(end) * ratio);
        return Color.rgb(r, g, b);
    }

    /**
     * 添加Logo到条码
     *
     * @param bmp 条码位图
     * @param cfg 生成配置
     * @return 添加Logo后的位图
     */
    private static Bitmap addLogo(@NonNull Bitmap bmp, @NonNull Config cfg) {
        if (cfg.logo == null || cfg.logo.isRecycled()) return bmp;
        try {
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            int size = bmp.getWidth();
            int logoSize = (int) (size * cfg.logoSizeRatio);
            Bitmap logo = Bitmap.createScaledBitmap(cfg.logo, logoSize, logoSize, true);
            if (cfg.logoRound) logo = getRoundBitmap(logo);
            if (cfg.logoBorder) drawLogoBorder(canvas, size, logoSize, cfg.logoBorderColor, paint);
            RectF rect = new RectF((size - logoSize) / 2f, (size - logoSize) / 2f, (size + logoSize) / 2f, (size + logoSize) / 2f);
            canvas.drawBitmap(logo, null, rect, paint);
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "Logo failed: " + e.getMessage());
            return bmp;
        }
    }

    /**
     * 将位图转换为圆形
     *
     * @param bmp 原始位图
     * @return 圆形位图
     */
    private static Bitmap getRoundBitmap(Bitmap bmp) {
        Bitmap output = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(output);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect r = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        RectF rf = new RectF(r);
        c.drawARGB(0, 0, 0, 0);
        c.drawRoundRect(rf, bmp.getWidth() / 2f, bmp.getHeight() / 2f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(bmp, r, r, p);
        return output;
    }

    /**
     * 绘制Logo边框
     *
     * @param c        画布
     * @param qrSize   二维码尺寸
     * @param logoSize Logo尺寸
     * @param color    边框颜色
     * @param p        画笔
     */
    private static void drawLogoBorder(Canvas c, int qrSize, int logoSize, int color, Paint p) {
        p.setColor(color);
        float borderWidth = logoSize * 0.1f;
        float radius = logoSize / 2f + borderWidth;
        c.drawCircle(qrSize / 2f, qrSize / 2f, radius, p);
    }

    /**
     * 添加水印到条码
     *
     * @param bmp 条码位图
     * @param cfg 生成配置
     * @return 添加水印后的位图
     */
    private static Bitmap addWatermark(@NonNull Bitmap bmp, @NonNull Config cfg) {
        if (cfg.watermarkText == null || cfg.watermarkText.isEmpty()) return bmp;
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(cfg.watermarkColor);
        paint.setTextSize(cfg.watermarkSize);
        paint.setTextAlign(Paint.Align.CENTER);
        float x = bmp.getWidth() * cfg.watermarkX;
        float y = bmp.getHeight() * cfg.watermarkY;
        canvas.drawText(cfg.watermarkText, x, y, paint);
        return bmp;
    }

    /**
     * 保存位图到文件
     *
     * @param context 上下文
     * @param bmp     要保存的位图
     * @param out     输出文件
     * @param quality 图片质量（0-100）
     * @return 保存成功返回true
     */
    public static boolean saveToFile(@NonNull Context context, @Nullable Bitmap bmp, @NonNull File out, int quality) {
        if (bmp == null || bmp.isRecycled()) {
            Log.w(TAG, "Invalid bitmap");
            return false;
        }
        if (Build.VERSION.SDK_INT >= 29)
            return saveToMediaStore(context, bmp, out.getName(), quality);
        if (requireStoragePermission(out) && !checkStoragePermission(context)) {
            Log.e(TAG, "No permission");
            return false;
        }
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.e(TAG, "Create dirs failed");
            return false;
        }
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Bitmap.CompressFormat fmt = guessFormat(out.getName());
            if (!bmp.compress(fmt, quality, fos)) {
                Log.e(TAG, "Compress failed");
                return false;
            }
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Save failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 通过MediaStore保存位图（Android 10+）
     *
     * @param ctx     上下文
     * @param bmp     要保存的位图
     * @param name    文件名
     * @param quality 图片质量
     * @return 保存成功返回true
     */
    private static boolean saveToMediaStore(@NonNull Context ctx, @NonNull Bitmap bmp, @NonNull String name, int quality) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZxingUtils");
            if (ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) == null)
                return false;
            OutputStream os = ctx.getContentResolver().openOutputStream(ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values));
            if (os != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, os);
                os.flush();
                os.close();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaStore failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 根据文件名猜测图片格式
     *
     * @param name 文件名
     * @return 图片压缩格式
     */
    private static Bitmap.CompressFormat guessFormat(@NonNull String name) {
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : "";
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
     * 判断是否需要存储权限
     *
     * @param out 输出文件
     * @return 需要权限返回true
     */
    private static boolean requireStoragePermission(File out) {
        return out.getAbsolutePath().startsWith("/storage/") || out.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    /**
     * 检查存储权限
     *
     * @param ctx 上下文
     * @return 有权限返回true
     */
    private static boolean checkStoragePermission(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return Environment.isExternalStorageManager() || hasPermission(ctx, Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return hasPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) && hasPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        else return hasPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * 检查是否有特定权限
     *
     * @param ctx  上下文
     * @param perm 权限名
     * @return 有权限返回true
     */
    private static boolean hasPermission(Context ctx, String perm) {
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 生成Base64编码的条码图片
     *
     * @param content 编码内容
     * @param size    图片尺寸
     * @param cfg     生成配置
     * @return Base64编码字符串，失败返回null
     */
    @Nullable
    public static String generateBase64(@NonNull String content, int size, @Nullable Config cfg) {
        Bitmap bmp = generateSync(content, size, cfg);
        if (bmp == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    /**
     * 处理内存溢出错误
     *
     * @param method 方法名
     */
    private static void handleOOMError(String method) {
        Log.e(TAG, "OOM in " + method + ", consider reducing size");
        System.gc();
    }

    /**
     * 在主线程投递成功结果
     *
     * @param cb  回调接口
     * @param bmp 生成的位图
     */
    private static void postSuccess(@NonNull Callback cb, @Nullable Bitmap bmp) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) cb.onSuccess(bmp);
        else new android.os.Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(bmp));
    }

    /**
     * 在主线程投递失败结果
     *
     * @param cb  回调接口
     * @param msg 错误信息
     */
    private static void postFailure(@NonNull Callback cb, @NonNull String msg) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) cb.onFailure(msg);
        else new android.os.Handler(Looper.getMainLooper()).post(() -> cb.onFailure(msg));
    }
}