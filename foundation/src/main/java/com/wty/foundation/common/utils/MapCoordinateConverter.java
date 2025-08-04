package com.wty.foundation.common.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地图坐标转换工具类，提供不同地图坐标系之间的转换方法
 */
public class MapCoordinateConverter {
    private static final String TAG = "MapCoordinateConverter";
    // 地球长半轴
    private static final double A = 6378245.0;
    // 扁率
    private static final double EE = 0.00669342162296594323;
    // 圆周率
    private static final double PI = 3.1415926535897932384626;

    /**
     * 私有构造函数，防止实例化
     */
    private MapCoordinateConverter() {
    }

    /**
     * 将 WGS84 坐标转换为 GCJ-02 坐标
     *
     * @param wgsLat WGS84 纬度
     * @param wgsLon WGS84 经度
     * @return 转换后的 GCJ-02 坐标数组 [纬度, 经度]
     */
    public static double[] wgs84ToGcj02(double wgsLat, double wgsLon) {
        return handleConversion("WGS84 to GCJ-02", wgsLat, wgsLon, () -> {
            if (!isInChina(wgsLat, wgsLon)) {
                return copyCoordinates(wgsLat, wgsLon);
            }

            // 计算偏移量
            double dLat = transformLat(wgsLon - 105.0, wgsLat - 35.0);
            double dLon = transformLng(wgsLon - 105.0, wgsLat - 35.0);
            double radLat = wgsLat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            // 转换纬度偏移为实际角度
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            // 转换经度偏移为实际角度
            dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);

            // 保留7位小数（约1cm精度）
            double mgLat = round(wgsLat + dLat, 7);
            double mgLon = round(wgsLon + dLon, 7);
            return new double[]{mgLat, mgLon};
        });
    }

    /**
     * 将 WGS84 坐标转换为 BD-09 坐标
     *
     * @param wgsLat WGS84 纬度
     * @param wgsLon WGS84 经度
     * @return 转换后的 BD-09 坐标数组 [纬度, 经度]
     */
    public static double[] wgs84ToBd09(double wgsLat, double wgsLon) {
        return handleConversion("WGS84 to BD-09", wgsLat, wgsLon, () -> {
            // 先转GCJ-02，再转BD-09
            double[] gcj = wgs84ToGcj02(wgsLat, wgsLon);
            return gcj02ToBd09(gcj[0], gcj[1]);
        });
    }

    /**
     * 将 GCJ-02 坐标转换为 WGS84 坐标
     *
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return 转换后的 WGS84 坐标数组 [纬度, 经度]
     */
    public static double[] gcj02ToWgs84(double gcjLat, double gcjLon) {
        return handleConversion("GCJ-02 to WGS84", gcjLat, gcjLon, () -> {
            if (!isInChina(gcjLat, gcjLon)) {
                return copyCoordinates(gcjLat, gcjLon);
            }

            // 计算偏移量（反向）
            double dLat = transformLat(gcjLon - 105.0, gcjLat - 35.0);
            double dLng = transformLng(gcjLon - 105.0, gcjLat - 35.0);
            double radLat = gcjLat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);

            // 反向计算原始坐标
            double mgLat = gcjLat + dLat;
            double mgLng = gcjLon + dLng;

            return new double[]{gcjLat * 2 - mgLat, gcjLon * 2 - mgLng};
        });
    }

    /**
     * 将 GCJ-02 坐标转换为 BD-09 坐标
     *
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return 转换后的 BD-09 坐标数组 [纬度, 经度]
     */
    public static double[] gcj02ToBd09(double gcjLat, double gcjLon) {
        return handleConversion("GCJ-02 to BD-09", gcjLat, gcjLon, () -> {
            double x = gcjLon;
            double y = gcjLat;
            // 百度加密偏移
            double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * PI);
            double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * PI);
            double bdLon = z * Math.cos(theta) + 0.0065;
            double bdLat = z * Math.sin(theta) + 0.006;
            return new double[]{round(bdLat, 7), round(bdLon, 7)};
        });
    }

    /**
     * 将 BD-09 坐标转换为 GCJ-02 坐标
     *
     * @param bdLat BD-09 纬度
     * @param bdLon BD-09 经度
     * @return 转换后的 GCJ-02 坐标数组 [纬度, 经度]
     */
    public static double[] bd09ToGcj02(double bdLat, double bdLon) {
        return handleConversion("BD-09 to GCJ-02", bdLat, bdLon, () -> {
            // 百度解密偏移
            double x = bdLon - 0.0065;
            double y = bdLat - 0.006;
            double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
            double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI);
            return new double[]{round(z * Math.sin(theta), 7), round(z * Math.cos(theta), 7)};
        });
    }

    /**
     * 将 BD-09 坐标转换为 WGS84 坐标
     *
     * @param bdLat BD-09 纬度
     * @param bdLon BD-09 经度
     * @return 转换后的 WGS84 坐标数组 [纬度, 经度]
     */
    public static double[] bd09ToWgs84(double bdLat, double bdLon) {
        return handleConversion("BD-09 to WGS84", bdLat, bdLon, () -> {
            // 先转GCJ-02，再转WGS84
            double[] gcj = bd09ToGcj02(bdLat, bdLon);
            return gcj02ToWgs84(gcj[0], gcj[1]);
        });
    }

    /**
     * 坐标转换函数式接口（用于统一异常处理）
     */
    private interface CoordinateConversion {
        double[] convert() throws Exception;
    }

    /**
     * 处理坐标转换，包含参数校验和异常处理
     *
     * @param conversionName 转换名称（用于日志）
     * @param lat            纬度
     * @param lon            经度
     * @param conversion     坐标转换实现
     * @return 转换后的坐标数组（转换失败返回原始坐标）
     */
    private static double[] handleConversion(String conversionName, double lat, double lon, CoordinateConversion conversion) {
        // 防御性拷贝原始坐标（避免外部修改影响）
        final double[] fallback = copyCoordinates(lat, lon);

        // 参数校验（经纬度范围、非特殊值）
        if (!isValidCoordinate(lat, lon)) {
            Log.e(TAG, String.format("Invalid input for %s: lat=%.6f, lon=%.6f", conversionName, lat, lon));
            return fallback;
        }

        // 执行转换并捕获异常
        try {
            return conversion.convert();
        } catch (Exception e) {
            Log.e(TAG, String.format("%s conversion failed for (%.6f,%.6f): %s", conversionName, lat, lon, e.getMessage()), e);
            return fallback;
        }
    }

    /**
     * 生成坐标防御性拷贝
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 坐标数组
     */
    private static double[] copyCoordinates(double lat, double lon) {
        return new double[]{lat, lon};
    }

    /**
     * 坐标有效性校验，包含特殊值检测
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 坐标是否有效
     */
    private static boolean isValidCoordinate(double lat, double lon) {
        return !(Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lon) || Double.isInfinite(lon) || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0);
    }

    /**
     * 检查坐标是否在中国境内
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 如果坐标在中国境内返回 true，否则返回 false
     */
    private static boolean isInChina(double lat, double lon) {
        return lon > 73.66 && lon < 135.05 && lat > 3.86 && lat < 53.55;
    }

    /**
     * 计算纬度偏移量
     *
     * @param x 经度差
     * @param y 纬度差
     * @return 纬度偏移量
     */
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 计算经度偏移量
     *
     * @param x 经度差
     * @param y 纬度差
     * @return 经度偏移量
     */
    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 四舍五入到指定的小数位数
     *
     * @param value  要四舍五入的值
     * @param places 保留小数位数
     * @return 四舍五入后的值
     */
    private static double round(double value, int places) {
        if (places < 0) {
            Log.e(TAG, "Invalid round places: " + places);
            return value;
        }

        try {
            // 使用BigDecimal避免double直接运算的精度误差
            return new BigDecimal(Double.toString(value)).setScale(places, RoundingMode.HALF_UP).doubleValue();
        } catch (NumberFormatException | ArithmeticException e) {
            Log.e(TAG, "Rounding error for value: " + value, e);
            return value;
        }
    }
}