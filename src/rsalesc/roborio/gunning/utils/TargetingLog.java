package rsalesc.roborio.gunning.utils;

import robocode.Rules;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class TargetingLog {
    public Point source;
    public double absBearing;
    public double velocity;
    public double distance;
    public double lateralVelocity;
    public double advancingVelocity;
    public double bulletPower;
    public double relativeHeading;
    public double accel;
    public long bulletsFired;
    public int direction;
    public int accelDirection;
    public double positiveEscape;
    public double negativeEscape;
    public double bafHeading;

    public long timeAccel;
    public long timeDecel;
    public long timeRevert;
    public long revertLast20;
    public double coveredLast20;
    public double gunHeat;
    public double lastMissGF;

    public long lastRun;
    public long run;

    public boolean aiming = false;

    // for miss
    public double hitAngle;
    public double hitDistance;
    public Point hitPosition;

    public Range preciseMea;

//    public double wallDistance;

    // helpers
    public Range getPreciseMea() {
        return preciseMea;
    }

    public double bft() {
        return distance / Rules.getBulletSpeed(bulletPower);
    }

    public double getGf(double offset) {
        return R.constrain(-1,
                this.direction * offset /
                        (this.direction * offset > 0 ? preciseMea.max : -preciseMea.min), +1);
    }

    public double getOffset(double gf) {
        return direction * gf * (gf > 0 ? preciseMea.max : -preciseMea.min);
    }

}
