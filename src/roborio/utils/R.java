package roborio.utils;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class R {
    public static final double  PI = Math.acos(-1);
    public static final double  HALF_PI = PI / 2;
    public static final double  DOUBLE_PI = PI * 2;
    public static final double  EPSILON = 1e-9;
    public static final int     WAVE_SNAPSHOT_DIAMETER = 4;
    public static final double  WAVE_EXTRA = 50;

    public static double sin(double radians) {
        double offset = radians - DOUBLE_PI * Math.floor((radians + PI) / DOUBLE_PI);
        if(Math.abs(offset) > HALF_PI)
            return -xsin((offset - Math.signum(offset) * PI) / HALF_PI);
        else
            return xsin(offset / HALF_PI);
    }

    public static double cos(double radians) {
        return R.sin(radians + HALF_PI);
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
        return fastAtan2(y, x);
    }

    public static double tan(double radians) {
        return Math.atan(radians);
    }

    public static double abs(double x) {
        return Math.abs(x);
    }

    public static double sqrt(double x) {
        return Math.sqrt(x);
    }

    public static double exp(double val) {
        return Math.exp(val);
    }

    public static double pow(final double a, final double b) {
        return Math.pow(a, b);
    }

    public static double exp7(double x) {
        return (362880+x*(362880+x*(181440+x*(60480+x*(15120+x*(3024+x*(504+x*(72+x*(9+x)))))))))*2.75573192e-6;
    }

    private static double xsin (double x) {
        double x2 = x * x;
        return ((((.00015148419 * x2
                - .00467376557) * x2
                + .07968967928) * x2
                - .64596371106) * x2
                + 1.57079631847) * x;
    }

    private static double fastAtan2(double y, double x) {
        if (x == 0.0f) {
            if (y > 0.0f) {
                return HALF_PI;
            }
            if (y == 0.0f) {
                return 0.0f;
            }
            return -HALF_PI;
        }

        final double atan;
        final double z = y / x;
        if (Math.abs(z) < 1.0f) {
            atan = z / (1.0f + 0.28f * z * z);
            if (x < 0.0f) {
                return (y < 0.0f) ? atan - PI : atan + PI;
            }
            return atan;
        } else {
            atan = HALF_PI - z / (z * z + 0.28f);
            return (y < 0.0f) ? atan - PI : atan;
        }
    }

    public static double logisticFunction(double x, double x0, double k) {
        return 1.0 / (1.0 + R.exp(-k * (x-x0)));
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
