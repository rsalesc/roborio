package roborio.gunning.utils;

import robocode.Rules;
import roborio.utils.Physics;
import roborio.utils.geo.Point;
import roborio.utils.geo.Range;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class TargetingLog {
    public Point source;
    public double absBearing;
    public double distance;
    public double lateralVelocity;
    public double advancingVelocity;
    public double bulletPower;
    public double accel;
    public long bulletsFired;
    public int direction;
    public int accelDirection;
    public double positiveEscape;
    public double negativeEscape;

    public long timeAccel;
    public long timeDecel;
    public long timeRevert;

    // for miss
    public double hitAngle;
    public double hitDistance;
    public Point hitPosition;

//    public double wallDistance;

    // helpers
    public Range getPreciseMea() {
        double mea = Physics.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));
        return new Range(-negativeEscape * mea, +positiveEscape * mea);
    }
}
