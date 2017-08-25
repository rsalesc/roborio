package rsalesc.roborio.movement.distancing;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public abstract class DistanceController {
    public static final int DESIRED_DISTANCE = 475;

    public abstract double getPerpendiculator(double distance);
    public abstract boolean shouldSurf(double distance);
}
