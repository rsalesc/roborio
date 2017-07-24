package roborio.utils;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class R {
    public static final double  PI = Math.acos(-1);
    public static final double  HALF_PI = PI / 2;
    public static final double  DOUBLE_PI = PI * 2;
    public static final double  EPSILON = 1e-9;
    public static final int     WAVE_SNAPSHOT_DIAMETER = 10;
    public static final double  WAVE_EXTRA = 50;

    public static double sin(double radians) {
        return Math.sin(radians);
    }

    public static double cos(double radians) {
        return Math.cos(radians);
    }

    public static double asin(double x) {
        return Math.asin(x);
    }

    public static double acos(double x) {
        return Math.acos(x);
    }

    public static double atan(double x) {
        return Math.atan(x);
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static double tan(double radians) {
        return Math.tan(radians);
    }

    public static double abs(double x) {
        return Math.abs(x);
    }

    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    public static double logisticFunction(double x, double x0, double k) {
        return 1.0 / (1.0 + Math.exp(-k * (x-x0)));
    }

    public static double logisticFunction(double x) {
        return logisticFunction(x, 0.0, 1.0);
    }

    public static double constrain(double min, double x, double max) {
        return Math.max(min, Math.min(max, x));
    }

    public static int constrain(int min, int x, int max) {
        return Math.max(min, Math.min(max, x));
    }

    public static boolean isBetween(int min, int x, int max) {
        return min <= x && x <= max;
    }

    public static boolean nearOrBetween(double min, double x, double max) {
        return min - EPSILON < x && x < max + EPSILON;
    }

    public static boolean isNear(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    public static boolean isNear(double a, double b, double error) {
        return Math.abs(a - b) < error;
    }
}
