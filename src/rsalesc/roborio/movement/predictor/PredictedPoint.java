package rsalesc.roborio.movement.predictor;

import robocode.util.Utils;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 07/08/17.
 */
public class PredictedPoint extends Point {
    public final double heading;
    public final double velocity;
    public final long time;
    public final int ahead;

    private PredictedPoint(double x, double y, double heading, double velocity, long time) {
        super(x, y);
        this.heading = heading;
        this.velocity = velocity;
        this.time = time;
        this.ahead = (int) Math.signum(velocity);
    }

    private PredictedPoint(Point point, double heading, double velocity, long time) {
        this(point.x, point.y, heading, velocity, time);
    }

    public PredictedPoint(double x, double y, double heading, double velocity, long time, int ahead) {
        super(x, y);
        this.heading = heading;
        this.velocity = velocity;
        this.time = time;
        this.ahead = ahead;
    }

    public PredictedPoint(Point point, double heading, double velocity, long time, int ahead) {
        this(point.x, point.y, heading, velocity, time, ahead);
    }

    public PredictedPoint tick(double newHeading, double newVelocity) {
        int newAhead = newVelocity == 0 && velocity != 0 || newVelocity * velocity > 0
                ? ahead
                : (int) Math.signum(newVelocity);
        return new PredictedPoint(this.project(newHeading, newVelocity), newHeading, newVelocity, time + 1, newAhead);
    }

    public PredictedPoint fakeTick(double newHeading, double newVelocity, double jumpAngle, double jumpSize) {
        int newAhead = newVelocity == 0 && velocity != 0 || newVelocity * velocity > 0
                ? ahead
                : (int) Math.signum(newVelocity);
        return new PredictedPoint(this.project(jumpAngle, jumpSize), newHeading, newVelocity, time + 1, newAhead);
    }

    public double getHeading() {
        return heading;
    }

    public double getVelocity() {
        return velocity;
    }

    public long getTime() {
        return time;
    }

    public int getAhead() {
        return ahead;
    }

    public int getDirection(Point from) {
        double head = getHeading();
        if(ahead < 0)
            head += R.PI;
        double absBearing = Physics.absoluteBearing(this, from);
        double off = Utils.normalRelativeAngle(head - absBearing);
        if(off > 0) return -1;
        else if(off < 0) return 1;
        else return 0;
    }

    public double getBafHeading() {
        if(getAhead() < 0)
            return Utils.normalAbsoluteAngle(heading + R.PI);
        return heading;
    }
}
