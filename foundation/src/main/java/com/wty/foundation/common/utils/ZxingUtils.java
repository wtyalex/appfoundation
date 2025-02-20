package com.wty.foundation.common.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于生成条形码或二维码，支持异步和同步生成方式，
 * 并可将生成的图片保存到文件，同时支持自定义配置。
 */
public class ZxingUtils {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final int DEFAULT_MARGIN = 1;
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.H;

    /**
     * 回调接口，用于处理生成结果。
     */
    public interface Callback {
        /**
         * 生成成功时调用。
         *
         * @param bitmap 生成的条形码或二维码图片
         */
        void onSuccess(Bitmap bitmap);

        /**
         * 生成失败时调用。
         *
         * @param e 异常信息
         */
        void onFailure(Exception e);
    }

    /**
     * 配置类，用于设置生成条形码或二维码的参数。
     */
    public static class Config {
        int fgColor = Color.BLACK; // 前景色
        int bgColor = Color.WHITE; // 背景色
        int margin = DEFAULT_MARGIN; // 边距
        ErrorCorrectionLevel errorCorrection = DEFAULT_ERROR_CORRECTION; // 纠错级别，主要用于QR码
        Bitmap logo; // 二维码中间的logo，仅用于QR码
        float logoSizeRatio = 0.2f; // Logo占二维码大小的比例，仅用于QR码
        boolean logoBorder = true; // 是否显示logo边框，仅用于QR码
        int logoBorderColor = Color.WHITE; // logo边框颜色，仅用于QR码
        BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE; // 条形码或二维码格式
    }

    /**
     * 异步生成条形码或二维码图片。
     *
     * @param content  要编码的内容
     * @param size     生成图片的尺寸
     * @param config   配置参数，可为null
     * @param callback 回调接口，处理生成结果
     */
    public static void generateAsync(String content, int size, Config config, Callback callback) {
        new GenerateTask(content, size, config, callback).execute();
    }

    /**
     * 同步生成条形码或二维码图片（建议在后台线程使用）。
     *
     * @param content 要编码的内容
     * @param size    生成图片的尺寸
     * @param config  配置参数，可为null
     * @return 生成的条形码或二维码Bitmap对象
     * @throws QRGenerationException 生成过程中发生错误时抛出
     */
    public static Bitmap generateSync(String content, int size, Config config) throws QRGenerationException {
        validateInput(content, size);

        try {
            Map<EncodeHintType, Object> hints = prepareHints(config);
            BitMatrix matrix = new MultiFormatWriter().encode(content, config.barcodeFormat, size, size, hints);
            return config.barcodeFormat == BarcodeFormat.QR_CODE ? addLogo(renderToBitmap(matrix, config), config) : renderToBitmap(matrix, config);
        } catch (WriterException | IllegalArgumentException e) {
            throw new QRGenerationException("条形码或二维码生成失败", e);
        }
    }

    /**
     * 将条形码或二维码保存到文件中（自动创建目录）。
     *
     * @param bitmap     要保存的条形码或二维码Bitmap对象
     * @param outputFile 输出文件
     * @param quality    压缩质量（仅对JPEG有效）
     * @throws IOException 保存过程中发生错误时抛出
     */
    public static void saveToFile(Bitmap bitmap, File outputFile, int quality) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos)) {
                throw new IOException("压缩Bitmap失败");
            }
        }
    }

    /**
     * 验证输入的内容和尺寸是否合法。
     *
     * @param content 要编码的内容
     * @param size    生成图片的尺寸
     */
    private static void validateInput(String content, int size) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("尺寸必须为正数");
        }
    }

    /**
     * 准备编码所需的提示信息。
     *
     * @param config 配置参数，可为null
     * @return 包含提示信息的Map
     */
    private static Map<EncodeHintType, Object> prepareHints(Config config) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
        hints.put(EncodeHintType.MARGIN, config != null ? config.margin : DEFAULT_MARGIN);
        if (config != null && config.errorCorrection != null && config.barcodeFormat == BarcodeFormat.QR_CODE) {
            hints.put(EncodeHintType.ERROR_CORRECTION, config.errorCorrection);
        }
        return hints;
    }

    /**
     * 将BitMatrix渲染为Bitmap对象。
     *
     * @param matrix 要渲染的BitMatrix
     * @param config 配置参数
     * @return 渲染后的Bitmap对象
     */
    private static Bitmap renderToBitmap(BitMatrix matrix, Config config) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        int fgColor = config.fgColor;
        int bgColor = config.bgColor;

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? fgColor : bgColor;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 为二维码添加logo。
     *
     * @param qrBitmap 二维码Bitmap对象
     * @param config   配置参数
     * @return 添加logo后的二维码Bitmap对象
     */
    private static Bitmap addLogo(Bitmap qrBitmap, Config config) {
        if (config.logo == null) return qrBitmap;

        int qrSize = qrBitmap.getWidth();
        int logoSize = (int) (qrSize * config.logoSizeRatio);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(config.logo, logoSize, logoSize, true);

        Canvas canvas = new Canvas(qrBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (config.logoBorder) {
            paint.setColor(config.logoBorderColor);
            float borderSize = logoSize * 0.1f;
            canvas.drawCircle(qrSize / 2f, qrSize / 2f, logoSize / 2f + borderSize, paint);
        }

        RectF logoRect = new RectF((qrSize - logoSize) / 2f, (qrSize - logoSize) / 2f, (qrSize + logoSize) / 2f, (qrSize + logoSize) / 2f);
        canvas.drawBitmap(scaledLogo, null, logoRect, paint);

        return qrBitmap;
    }

    /**
     * 异步任务类，用于异步生成条形码或二维码。
     */
    private static class GenerateTask extends AsyncTask<Void, Void, Bitmap> {
        private final String content;
        private final int size;
        private final Config config;
        private final Callback callback;
        private Exception exception;

        GenerateTask(String content, int size, Config config, Callback callback) {
            this.content = content;
            this.size = size;
            this.config = config;
            this.callback = callback;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                return generateSync(content, size, config);
            } catch (QRGenerationException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onFailure(exception != null ? exception : new Exception("未知错误"));
            }
        }
    }

    /**
     * 自定义异常类，用于表示条形码或二维码生成过程中发生的错误。
     */
    public static class QRGenerationException extends Exception {
        QRGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}