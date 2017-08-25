package rsalesc.roborio.movement.distancing;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public class DefaultDistanceController extends DistanceController {
    @Override
    public double getPerpendiculator(double distance) {
        if(distance < 250)
            return R.HALF_PI - 1.5;
        return R.HALF_PI - (1 - (distance / DESIRED_DISTANCE));
    }

    @Override
    public boolean shouldSurf(double distance) {
        return distance >= 250;
    }
}
