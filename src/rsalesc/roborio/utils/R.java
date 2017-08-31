package rsalesc.roborio.utils;

import robocode.Bullet;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.waves.EnemyFireWave;
import rsalesc.roborio.utils.waves.Wave;
import rsalesc.roborio.utils.waves.WaveCollection;

import java.text.DecimalFormat;
import java.util.List;

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
    private static final DecimalFormat PERCENTAGE_FORMATTER = new DecimalFormat("#.##");

    public static double sin(double radians) {
//        double offset = radians - DOUBLE_PI * Math.floor((radians + PI) / DOUBLE_PI);
//        if(Math.abs(offset) > HALF_PI)
//            return -xsin((offset - Math.signum(offset) * PI) / HALF_PI);
//        else
//            return xsin(offset / HALF_PI);
        return Math.sin(radians);
    }

    public static double cos(double radians) {
//        return R.sin(radians + HALF_PI);
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
//        return fastAtan2(y, x);
        return Math.atan2(y, x);
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
//        return fastexp((float) val);
        return Math.exp(val);
    }

    public static double pow(final double a, final double b) {
        return Math.pow(a, b);
    }

    public static double exp7(double x) {
        return (362880+x*(362880+x*(181440+x*(60480+x*(15120+x*(3024+x*(504+x*(72+x*(9+x)))))))))*2.75573192e-6;
    }

    private static float fastpow2(float pp) {
        float p = Math.abs(pp);
        float offset = p < 0.f ? 1.0f : 0.0f;
        int w = p < -126.f ? -126 : (int) p;
        float z = p - w + offset;
        int raw = (int) ((1 << 23) * (p + 121.2740838f + 27.728023f / (4.84252568f - z) - 1.49012907f * z));
        if(pp < -R.EPSILON)
            return 1.0f / Float.intBitsToFloat(raw);
        return Float.intBitsToFloat(raw);
    }

    private static float fastexp(float p) {
        return fastpow2(1.442695040f * p);
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

    public static double gaussKernel(double x) {
        return R.pow(1.65, -0.5 * x);
    }

    public static double cubicKernel(double x) { return Math.max(0, 1.0 - Math.abs(x*x*x)); }

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

    public static boolean isBetween(long min, long x, long max) {
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

    public static double transposeAngle(double angle) {
        return Utils.normalAbsoluteAngle(-Utils.normalRelativeAngle(angle) + R.HALF_PI);
    }

    public static double getWallEscape(AxisRectangle field, Point point, double heading) {
        return Math.min(getVerticalEscapeAngle(field, point, heading),
                getVerticalEscapeAngle(field.transposed(), point.transposed(), R.transposeAngle(heading)));
    }

    public static double getVerticalEscapeAngle(AxisRectangle field, Point point, double heading) {
        if(R.isNear(heading, R.HALF_PI) || R.isNear(heading, R.HALF_PI * 3))
            return Double.POSITIVE_INFINITY;
        else if(heading < R.HALF_PI || heading > 3 * R.HALF_PI)
            return (field.getHeight() - point.y) / R.cos(heading);
        else
            return -point.y / R.cos(heading);
    }

    public static String formattedPercentage(double v) {
        return PERCENTAGE_FORMATTER.format(v * 100).replace(",", ".") + " %";
    }

    public static AngularRange preciseIntersection(EnemyLog log, Wave wave, long passTime, double refAngle) {
        long curTime = passTime;
        while(wave.hasTouchedRobot(log.atLeastAt(curTime).getPoint(), curTime))
            curTime--;

        AngularRange range = new AngularRange(refAngle, new Range());
        ComplexEnemyRobot enemy = log.atLeastAt(curTime);
        do {
            ComplexEnemyRobot nextEnemy = log.atLeastAt(curTime + 1);
            AxisRectangle botRect = nextEnemy.getHitBox();

            for(Point corner : botRect.getCorners()) {
                if(wave.hasPassed(corner, nextEnemy.getTime()) && !wave.hasPassed(corner, enemy.getTime())) {
                    range.pushAngle(wave.getAngle(corner));
                }
            }

            for(Point intersect : wave.getCircle(enemy.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            for(Point intersect : wave.getCircle(nextEnemy.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            enemy = nextEnemy;
            curTime++;
        } while (curTime != passTime);

        if(range.isEmpty())
            return null;

        return range;
    }

    public static AngularRange preciseIntersection(MyLog log, Wave wave, long passTime, double refAngle) {
        long curTime = passTime;
        while(wave.hasTouchedRobot(log.atLeastAt(curTime).getPoint(), curTime))
            curTime--;

        AngularRange range = new AngularRange(refAngle, new Range());
        MyRobot me = log.atLeastAt(curTime);
        do {
            MyRobot nextEnemy = log.atLeastAt(curTime + 1);
            AxisRectangle botRect = nextEnemy.getHitBox();

            for(Point corner : botRect.getCorners()) {
                if(wave.hasPassed(corner, nextEnemy.getTime()) && !wave.hasPassed(corner, me.getTime())) {
                    range.pushAngle(wave.getAngle(corner));
                }
            }

            for(Point intersect : wave.getCircle(me.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            for(Point intersect : wave.getCircle(nextEnemy.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            me = nextEnemy;
            curTime++;
        } while (curTime != passTime);

        if(range.isEmpty())
            return null;

        return range;
    }

    public static AngularRange preciseIntersection(Wave wave, List<PredictedPoint> predicted) {
        double refAngle = R.getLast(predicted).getHeading();

        int ptr = predicted.size() - 1;
        while(ptr > 0 && wave.hasTouchedRobot(predicted.get(ptr), predicted.get(ptr).getTime()))
            ptr--;

        AngularRange range = new AngularRange(refAngle, new Range());
        PredictedPoint me = predicted.get(ptr);

        while(ptr + 1 < predicted.size()) {
            PredictedPoint nextEnemy = predicted.get(ptr+1);
            AxisRectangle botRect = new AxisRectangle(nextEnemy, Physics.BOT_WIDTH * 2);

            for(Point corner : botRect.getCorners()) {
                if(wave.hasPassed(corner, nextEnemy.getTime()) && !wave.hasPassed(corner, me.getTime())) {
                    range.pushAngle(wave.getAngle(corner));
                }
            }

            for(Point intersect : wave.getCircle(me.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            for(Point intersect : wave.getCircle(nextEnemy.getTime()).intersect(botRect)) {
                range.pushAngle(wave.getAngle(intersect));
            }

            me = nextEnemy;
            ptr++;
        }

        if(range.isEmpty())
            return null;

        return range;
    }

    public static double zeroNan(double v) {
        if(Double.isNaN(v))
            return 0.0;
        return v;
    }

    public static <T> T getLast(List<T> col) {
        if(col.size() == 0)
            return null;
        return col.get(col.size() - 1);
    }

    public static <T> T getFirst(List<T> col) {
        if(col.size() == 0)
            return null;
        return col.get(0);
    }

    public static boolean strictlyBetween(double min, double x, double max) {
        return R.nearOrBetween(min, x, max) && !R.isNear(x, min) && !R.isNear(x, max);
    }

    public static double basicSurferRounding(double x) {
        return Math.max(Math.min(x, 0.15), Math.floor(x / 0.05) * 0.05 - (x < 0.05 ? 0.05 : 0));
    }

    public static Wave getFirer(WaveCollection waves, Bullet bullet, long time) {
        for(Wave wave : waves) {
            if(wave instanceof EnemyFireWave
                    && !((EnemyFireWave) wave).isImaginary()
                    && ((EnemyFireWave) wave).wasFiredBy(bullet, time)) {
                return wave;
            }
        }

        return null;
    }
}
