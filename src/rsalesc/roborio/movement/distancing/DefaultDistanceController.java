package rsalesc.roborio.movement.distancing;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public class DefaultDistanceController extends DistanceController {
    @Override
    public double getPerpendiculator(double distance) {
        return R.HALF_PI +
                R.constrain(-R.HALF_PI/2,
                        (distance - DESIRED_DISTANCE) / DESIRED_DISTANCE * 0.7,
                        +R.HALF_PI/2);
    }

    @Override
    public boolean shouldSurf(double distance) {
        return true;
    }
}
