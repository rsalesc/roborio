package roborio.utils;

import robocode.Robot;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public abstract class Physics {
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
     * @param robot
     * @param absBearing
     * @param velocity
     * @param heading
     * @return
     */
    public static double getLateralVelocityFromStationary(Robot robot, double absBearing, double velocity, double heading) {
        return Math.sin(heading - absBearing) * velocity;
    }

    /** Return angular velocity, assuming that the enemy
     * is doing a circular movement around you and you are stationary.
     * Positive means clockwise.
     * @param distance
     * @param velocity
     * @return
     */
    public static double getAngularVelocityFromStationary(Robot robot, double absBearing, double distance,
                                                          double velocity, double heading) {
        return (velocity / distance) * Math.signum(getLateralVelocityFromStationary(robot, absBearing, velocity, heading));
    }

    public static double getApproachingVelocityFromStationary(Robot robot, double absBearing, double distance,
                                                double velocity, double heading) {
        return -Math.cos(heading - absBearing) * velocity;
    }
}
