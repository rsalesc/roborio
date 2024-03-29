package rsalesc.roborio.movement.distancing;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class FallbackDistanceController extends DistanceController {
    @Override
    public double getPerpendiculator(double distance) {
        return R.HALF_PI +
                R.constrain(-1.5,
                        (distance - DESIRED_DISTANCE) / DESIRED_DISTANCE * 1.4,
                        +1.5);
    }

    @Override
    public boolean shouldSurf(double distance) {
        return true;
    }
}
