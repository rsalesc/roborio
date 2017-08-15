package rsalesc.roborio.movement.predictor;

import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 07/08/17.
 */
public class PredictedPoint extends Point {
    public final double heading;
    public final double velocity;
    public final long time;

    public PredictedPoint(double x, double y, double heading, double velocity, long time) {
        super(x, y);
        this.heading = heading;
        this.velocity = velocity;
        this.time = time;
    }

    public PredictedPoint(Point point, double heading, double velocity, long time) {
        this(point.x, point.y, heading, velocity, time);
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
}
