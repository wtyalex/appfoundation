package com.wty.foundation.common.utils;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapUtils {
    // 日志标签
    private static final String TAG = "MapUtils";
    // 日志记录器
    private static final Logger logger = Logger.getLogger("MapUtils");
    // 地球半径，单位：米
    private static final double EARTH_RADIUS = 6371000.0;
    // 默认最大尝试次数
    private static final int DEFAULT_MAX_ATTEMPTS = 200;
    // 坐标精度
    private static final int COORDINATE_PRECISION = 7;
    // 默认地理坐标点
    private static final GeoPoint DEFAULT_POINT = new GeoPoint(0.0, 0.0);
    // 默认半径
    private static final double DEFAULT_RADIUS = 50.0;
    // 默认数量
    private static final int DEFAULT_COUNT = 10;
    // 最大数量限制，防止滥用
    private static final int MAX_COUNT = 1000;

    // 私有构造函数，防止实例化
    private MapUtils() {

    }

    /**
     * 安全地生成一个地理坐标点
     *
     * @param center       中心点
     * @param radiusMeters 半径，单位：米
     * @return 生成的地理坐标点，若失败则返回默认点
     */
    public static GeoPoint safeGenerate(GeoPoint center, double radiusMeters) {
        if (!isValidCoordinate(center)) {
            logInvalidParameter("safeGenerate", "center", center);
            center = DEFAULT_POINT;
        }

        double safeRadius = correctRadius(radiusMeters);
        try {
            return safeGenerateInternal(center, safeRadius, Collections.emptySet(), 0.0, 1);
        } catch (Exception e) {
            logError("safeGenerate", "Failed to generate safe GeoPoint, returning default point: " + e.getMessage());
            return DEFAULT_POINT;
        }
    }

    /**
     * 安全地生成一个唯一的地理坐标点
     *
     * @param center       中心点
     * @param radiusMeters 半径，单位：米
     * @param existing     已存在的地理坐标点集合
     * @param minDistance  最小距离，单位：米
     * @return 生成的地理坐标点
     */
    public static GeoPoint safeGenerateUnique(GeoPoint center, double radiusMeters, Set<GeoPoint> existing, double minDistance) {
        GeoPoint safeCenter = handleNull(center, DEFAULT_POINT);
        double safeRadius = correctRadius(radiusMeters);
        Set<GeoPoint> safeExisting = handleNullCollection(existing);
        double safeMinDistance = correctDistance(minDistance);

        try {
            return safeGenerateInternal(safeCenter, safeRadius, safeExisting, safeMinDistance, DEFAULT_MAX_ATTEMPTS);
        } catch (Exception e) {
            logError("safeGenerateUnique", "Failed to generate unique GeoPoint, returning default point: " + e.getMessage());
            return DEFAULT_POINT;
        }
    }

    /**
     * 安全地批量生成地理坐标点
     *
     * @param center       中心点
     * @param radiusMeters 半径，单位：米
     * @param count        生成的数量
     * @param minDistance  最小距离，单位：米
     * @return 生成的地理坐标点列表
     */
    public static List<GeoPoint> safeBatchGenerate(GeoPoint center, double radiusMeters, int count, double minDistance) {
        GeoPoint safeCenter = handleNull(center, DEFAULT_POINT);
        double safeRadius = correctRadius(radiusMeters);
        int safeCount = correctCount(count);
        double safeMinDistance = correctDistance(minDistance);

        // 用于存储唯一的地理坐标点
        final Set<GeoPoint> uniqueSet;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uniqueSet = ConcurrentHashMap.newKeySet();
        } else {
            uniqueSet = Collections.synchronizedSet(Collections.newSetFromMap(new ConcurrentHashMap<>()));
        }

        // 存储最终结果
        List<GeoPoint> result = new ArrayList<>(safeCount);

        for (int i = 0; i < safeCount; i++) {
            try {
                // 尝试生成一个唯一的地理坐标点
                GeoPoint point = safeGenerateInternal(safeCenter, safeRadius, uniqueSet, safeMinDistance, DEFAULT_MAX_ATTEMPTS);
                result.add(point);
                uniqueSet.add(point); // 添加到set,用于保证批量生产的唯一性

            } catch (Exception e) {
                // 若生成失败，记录错误日志并退出循环
                logError("safeBatchGenerate", "无法生成足够唯一坐标，已生成数量：" + result.size() + ". " + e.getMessage());
                break;
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * 内部方法，安全地生成地理坐标点。
     *
     * @param center      中心点
     * @param radius      半径，单位：米
     * @param existing    已存在的地理坐标点集合
     * @param minDistance 最小距离，单位：米
     * @param maxAttempts 最大尝试次数
     * @return 生成的地理坐标点
     */
    private static GeoPoint safeGenerateInternal(GeoPoint center, double radius, Set<GeoPoint> existing, double minDistance, int maxAttempts) {
        // 检查中心点坐标是否有效
        if (!isValidCoordinate(center)) {
            logInvalidParameter("safeGenerateInternal", "center", center);
            return DEFAULT_POINT;
        }
        // 获取线程本地随机数生成器
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int attempts = 0;

        while (attempts++ < maxAttempts) {
            try {
                // 生成一个候选地理坐标点
                GeoPoint candidate = generateCoordinate(center, radius, random);
                // 检查候选点是否有效
                if (isValidCandidate(candidate, existing, minDistance)) {
                    return candidate;
                }
            } catch (Exception e) {
                // 若生成过程中出现异常，记录错误日志并返回默认值
                logError("safeGenerateInternal", "坐标生成异常: " + e.getMessage());
                return DEFAULT_POINT;
            }
        }

        // 超过最大尝试次数，返回默认值
        logError("safeGenerateInternal", "无法在 " + maxAttempts + " 次尝试后生成唯一坐标，返回默认坐标.");
        return DEFAULT_POINT;
    }

    /**
     * 生成一个地理坐标点
     *
     * @param center 中心点
     * @param radius 半径，单位：米
     * @param random 随机数生成器
     * @return 生成的地理坐标点
     */
    private static GeoPoint generateCoordinate(GeoPoint center, double radius, ThreadLocalRandom random) {
        // 生成一个随机角度
        double angle = random.nextDouble() * 2 * Math.PI;
        // 生成一个随机距离
        double distance = radius * Math.sqrt(random.nextDouble());

        // 计算纬度偏移量
        double latOffset = (distance * Math.cos(angle)) / 111319.9;
        // 计算经度偏移量
        double lonOffset = (distance * Math.sin(angle)) / (111319.9 * Math.cos(Math.toRadians(center.latitude)));

        return new GeoPoint(center.latitude + latOffset, center.longitude + lonOffset);
    }

    /**
     * 计算两个地理坐标点之间的方位角
     *
     * @param a 第一个地理坐标点
     * @param b 第二个地理坐标点
     * @return 方位角，单位：度，计算失败，返回默认值
     */
    public static double calculateBearing(GeoPoint a, GeoPoint b) {
        if (a == null || b == null) {
            logInvalidParameter("calculateBearing", "GeoPoint", (a == null ? "a" : "b"));
            return 0.0;
        }

        try {
            double lat1 = Math.toRadians(a.latitude);
            double lon1 = Math.toRadians(a.longitude);
            double lat2 = Math.toRadians(b.latitude);
            double lon2 = Math.toRadians(b.longitude);

            double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
            double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
            double bearing = Math.atan2(y, x);
            return (bearing + Math.PI) % (2 * Math.PI) * 180 / Math.PI;
        } catch (Exception e) {
            logError("calculateBearing", "Failed to calculate bearing: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * 判断一个地理坐标点是否在指定圆内
     *
     * @param point  待判断的地理坐标点
     * @param center 圆心
     * @param radius 半径，单位：米
     * @return 若在圆内返回true，否则返回false
     */
    public static boolean isPointInCircle(GeoPoint point, GeoPoint center, double radius) {
        if (point == null || center == null) {
            logInvalidParameter("isPointInCircle", "GeoPoint", (point == null ? "point" : "center"));
            return false;
        }
        double safeRadius = correctRadius(radius);
        double distance = calculateDistance(center, point);
        return distance <= safeRadius + 1e-9; // 考虑浮点误差
    }

    /**
     * 检查地理坐标点是否有效
     *
     * @param point 地理坐标点
     * @return 若有效返回true，否则返回false
     */
    private static boolean isValidCoordinate(GeoPoint point) {
        return point != null && point.latitude >= -90.0 && point.latitude <= 90.0 && point.longitude >= -180.0 && point.longitude <= 180.0;
    }

    /**
     * 检查候选地理坐标点是否有效
     *
     * @param candidate   候选地理坐标点
     * @param existing    已存在的地理坐标点集合
     * @param minDistance 最小距离，单位：米
     * @return 若有效返回true，否则返回false
     */
    private static boolean isValidCandidate(GeoPoint candidate, Set<GeoPoint> existing, double minDistance) {
        if (candidate == null || !isValidCoordinate(candidate)) return false;
        if (existing.isEmpty()) return true;

        for (GeoPoint p : existing) {
            if (calculateDistance(p, candidate) < minDistance) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算两个地理坐标点之间的距离
     *
     * @param a 第一个地理坐标点
     * @param b 第二个地理坐标点
     * @return 距离，单位：米，若计算失败返回Double.MAX_VALUE
     */
    private static double calculateDistance(GeoPoint a, GeoPoint b) {
        if (a == null || b == null) {
            logInvalidParameter("calculateDistance", "GeoPoint", (a == null ? "a" : "b"));
            return Double.MAX_VALUE;
        }
        try {
            return calculateHaversine(a, b);
        } catch (Exception e) {
            logError("calculateDistance", "距离计算失败: " + e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    /**
     * 使用Haversine公式计算两个地理坐标点之间的距离
     *
     * @param a 第一个地理坐标点
     * @param b 第二个地理坐标点
     * @return 距离，单位：米
     */
    private static double calculateHaversine(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.latitude);
        double lon1 = Math.toRadians(a.longitude);
        double lat2 = Math.toRadians(b.latitude);
        double lon2 = Math.toRadians(b.longitude);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double sinHalfLat = Math.sin(dLat / 2);
        double sinHalfLon = Math.sin(dLon / 2);

        double aa = sinHalfLat * sinHalfLat + Math.cos(lat1) * Math.cos(lat2) * sinHalfLon * sinHalfLon;

        return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
    }

    /**
     * 处理空的地理坐标点
     *
     * @param input        输入的地理坐标点
     * @param defaultValue 默认值
     * @return 若输入不为空则返回输入，否则返回默认值
     */
    private static GeoPoint handleNull(GeoPoint input, GeoPoint defaultValue) {
        return input != null ? input : defaultValue;
    }

    /**
     * 处理空的地理坐标点集合
     *
     * @param input 输入的地理坐标点集合
     * @return 若输入不为空则返回输入，否则返回空集合
     */
    private static Set<GeoPoint> handleNullCollection(Set<GeoPoint> input) {
        return input != null ? input : Collections.emptySet();
    }

    /**
     * 修正半径值
     *
     * @param radius 输入的半径值
     * @return 若半径大于0则返回半径，否则返回默认值50米
     */
    private static double correctRadius(double radius) {
        return radius > 0 ? radius : DEFAULT_RADIUS;
    }

    /**
     * 修正距离值
     *
     * @param distance 输入的距离值
     * @return 若距离大于等于0则返回距离，否则返回0
     */
    private static double correctDistance(double distance) {
        return distance >= 0 ? distance : 0.0;
    }

    /**
     * 修正生成数量
     *
     * @param count 输入的生成数量
     * @return 若数量大于0则返回数量和1000中的较小值，否则返回10
     */
    private static int correctCount(int count) {
        return count > 0 ? Math.min(count, MAX_COUNT) : DEFAULT_COUNT;
    }

    /**
     * 记录无效参数的日志
     *
     * @param methodName 方法名
     * @param paramName  参数名
     * @param value      参数值
     */
    private static void logInvalidParameter(String methodName, String paramName, Object value) {
        Log.w(TAG, "[" + methodName + "] Invalid parameter: " + paramName + " = " + value + ". Using default value.");
        logger.log(Level.WARNING, "[" + methodName + "] Invalid parameter: " + paramName + " = " + value + ". Using default value.");
    }

    /**
     * 记录错误日志
     *
     * @param message 错误信息
     */
    private static void logError(String methodName, String message) {
        Log.e(TAG, "[" + methodName + "] " + message);
        logger.log(Level.SEVERE, "[" + methodName + "] " + message);
    }

    public static final class GeoPoint {
        // 纬度
        public final double latitude;
        // 经度
        public final double longitude;
        // 哈希值
        private final int hash;

        /**
         * 构造函数
         *
         * @param latitude  纬度
         * @param longitude 经度
         */
        public GeoPoint(double latitude, double longitude) {
            this.latitude = round(latitude);
            this.longitude = round(longitude);
            this.hash = Objects.hash(BigDecimal.valueOf(this.latitude), BigDecimal.valueOf(this.longitude));
        }

        /**
         * 对数值进行四舍五入
         *
         * @param value 输入的数值
         * @return 四舍五入后的数值
         */
        private static double round(double value) {
            return BigDecimal.valueOf(value).setScale(COORDINATE_PRECISION, RoundingMode.HALF_UP).doubleValue();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GeoPoint)) return false;
            GeoPoint other = (GeoPoint) o;
            return Double.compare(this.latitude, other.latitude) == 0 && Double.compare(this.longitude, other.longitude) == 0;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%.7f,%.7f", latitude, longitude);
        }
    }
}