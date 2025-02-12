package com.wty.foundation.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MapCoordinateConverter {
    private static final String TAG = "MapCoordinateConverter";
    private static final double A = 6378245.0; // 长半轴
    private static final double EE = 0.00669342162296594323; // 扁率
    private static final double PI = 3.1415926535897932384626; // 更精确的圆周率

    // 私有构造器防止实例化
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
        validateCoordinates(wgsLat, wgsLon);
        if (isInChina(wgsLat, wgsLon)) {
            double dLat = transformLat(wgsLon - 105.0, wgsLat - 35.0);
            double dLon = transformLng(wgsLon - 105.0, wgsLat - 35.0);
            double radLat = wgsLat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);

            // 应用四舍五入以匹配坐标变换的精度
            double mgLat = round(wgsLat + dLat, 7);
            double mgLon = round(wgsLon + dLon, 7);
            return new double[]{mgLat, mgLon};
        } else {
            return new double[]{wgsLat, wgsLon};
        }
    }

    /**
     * 将 GCJ-02 坐标转换为 WGS84 坐标
     *
     * @param gcjLat GCJ-02 纬度
     * @param gcjLon GCJ-02 经度
     * @return 转换后的 WGS84 坐标数组 [纬度, 经度]
     */
    public static double[] gcj02ToWgs84(double gcjLat, double gcjLon) {
        validateCoordinates(gcjLat, gcjLon);
        if (isInChina(gcjLat, gcjLon)) {
            double dLat = transformLat(gcjLon - 105.0, gcjLat - 35.0);
            double dLng = transformLng(gcjLon - 105.0, gcjLat - 35.0);
            double radLat = gcjLat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);

            double mgLat = gcjLat + dLat;
            double mgLng = gcjLon + dLng;

            return new double[]{gcjLat * 2 - mgLat, gcjLon * 2 - mgLng};
        } else {
            return new double[]{gcjLat, gcjLon};
        }
    }

    /**
     * 将 BD-09 坐标转换为 GCJ-02 坐标
     *
     * @param bdLat BD-09 纬度
     * @param bdLon BD-09 经度
     * @return 转换后的 GCJ-02 坐标数组 [纬度, 经度]
     */
    public static double[] bd09ToGcj02(double bdLat, double bdLon) {
        validateCoordinates(bdLat, bdLon);
        double x = bdLon - 0.0065;
        double y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI);
        return new double[]{z * Math.sin(theta), z * Math.cos(theta)};
    }

    /**
     * 将 BD-09 坐标转换为 WGS84 坐标
     *
     * @param bdLat BD-09 纬度
     * @param bdLon BD-09 经度
     * @return 转换后的 WGS84 坐标数组 [纬度, 经度]
     */
    public static double[] bd09ToWgs84(double bdLat, double bdLon) {
        double[] gcj = bd09ToGcj02(bdLat, bdLon);
        return gcj02ToWgs84(gcj[0], gcj[1]);
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
     * @param value  待四舍五入的值
     * @param places 小数位数
     * @return 四舍五入后的值
     */
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 验证坐标值的有效性
     *
     * @param lat 纬度
     * @param lon 经度
     * @throws IllegalArgumentException 如果坐标无效
     */
    private static void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates: latitude must be between -90 and 90, longitude must be between -180 and 180.");
        }
    }
}