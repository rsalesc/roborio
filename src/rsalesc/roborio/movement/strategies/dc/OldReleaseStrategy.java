package rsalesc.roborio.movement.strategies.dc;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class OldReleaseStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.max(f.bft() / 80, 1),
                Math.max(f.lateralVelocity / 8, 1),
                (f.accel + 1) / 2,
                ((f.bulletPower - 1) / 2.9),
                R.constrain(0, f.getPreciseMea().max / f.getMea(), 1.25),
                R.constrain(0, -f.getPreciseMea().min / f.getMea(), 1.25)
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{2, 3.5, 4, 2.5, 1.5, 0.75};
    }
}
