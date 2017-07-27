package roborio.utils;

import robocode.Robot;
import robocode.Rules;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public abstract class Physics {
    public static final double  MAX_VELOCITY = Rules.MAX_VELOCITY;
    public static final double  MAX_TURN_RATE = Rules.MAX_TURN_RATE;
    public static final double  MAX_POWER = 3.0;
    public static final double  MIN_POWER = 0.1;
    public static final double  BOT_WIDTH = 18;

    public static double absoluteBearing(Point source, Point dest) {
        return new Point(source, dest).absoluteBearing();
    }

    public static double getX(Robot robot, double absBearing, double distance) {
        return robot.getX() + Math.sin(absBearing) * distance;
    }

    public static double getY(Robot robot, double absBearing, double distance) {
        return robot.getY() + Math.cos(absBearing) * distance;
    }

    /** Returns lateral velocity relative to robot, but
     * assuming it as a stationary bot.
     * Positive means clockwise/to the right.
     * @param absBearing
     * @param velocity
     * @param heading
     * @return
     */
    public static double getLateralVelocityFromStationary(double absBearing, double velocity, double heading) {
        return Math.sin(heading - absBearing) * velocity;
    }

    /** Return angular velocity, assuming that the enemy
     * is doing a circular movement around you and you are stationary.
     * Positive means clockwise.
     * @param distance
     * @param velocity
     * @return
     */
    public static double getAngularVelocityFromStationary(double absBearing, double distance,
                                                          double velocity, double heading) {
        return (velocity / distance) * Math.signum(getLateralVelocityFromStationary(absBearing, velocity, heading));
    }

    public static double getApproachingVelocityFromStationary(double absBearing,
                                                              double velocity, double heading) {
        return -Math.cos(heading - absBearing) * velocity;
    }

    public static double maxEscapeAngle(double velocity) {
        return Math.asin(MAX_VELOCITY / Math.abs(velocity));
    }

    public static double maxTurningRate(double velocity) {
        return Math.toRadians(MAX_TURN_RATE - 0.75 * Math.abs(velocity));
    }

    public static double bulletVelocity(double power) {
        return 20. - power*3;
    }

    public static double bulletPower(double velocity){
        return (20.0 - velocity) / 3;
    }
}
