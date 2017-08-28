package rsalesc.roborio.gunning.utils;

import robocode.Rules;
import robocode.util.Utils;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
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

    public long time;
    public long timeAccel;
    public long timeDecel;
    public long timeRevert;
    public double displaceLast10;
    public double displaceLast20;
    public double displaceLast40;
    public double displaceLast80;
    public double displaceLast160;

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
    public AngularRange preciseIntersection;

//    public double wallDistance;

    // helpers
    public Range getPreciseMea() {
        return preciseMea;
    }

    public double getMea() {
        return Physics.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));
    }

    public double bft() {
        return distance / (Rules.getBulletSpeed(bulletPower) + 1e-8);
    }

    public double getUnconstrainedGfFromAngle(double angle) {
        return getUnconstrainedGf(Utils.normalRelativeAngle(angle - absBearing));
    }

    public double getGfFromAngle(double angle) {
        return R.constrain(-1, getUnconstrainedGfFromAngle(angle), +1);
    }

    public double getGf(double offset) {
        return R.constrain(-1, getUnconstrainedGf(offset), +1);
    }

    public double getUnconstrainedGf(double offset) {
        return R.zeroNan(this.direction * offset /
                        (this.direction * offset > 0 ? preciseMea.max : -preciseMea.min));
    }

    public double getOffset(double gf) {
        return R.zeroNan(direction * gf * (gf > 0 ? preciseMea.max : -preciseMea.min));
    }

    public double heat() {
        double heatGenerated = 1.0 + bulletPower / 5;
        return Math.min(gunHeat / 0.1, Math.max(heatGenerated - gunHeat, 0) / 0.1) / 16;
    }

    public double getZeroGf() {
        return absBearing;
    }

}
