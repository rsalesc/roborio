package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public class AntiSurferStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.min(Math.abs(f.lateralVelocity) / 8, 1),
                R.constrain(0, (f.advancingVelocity / 8 + 1) / 2, 1),
                Math.min(f.distance / 800, 1),
                R.constrain(0, (f.accel + 1) / 2, 1),
                R.constrain(0, f.displaceLast10 / 80, 1),
                R.constrain(0, f.getPreciseMea().max / f.getMea(), 1),
                R.constrain(0, -f.getPreciseMea().min / f.getMea(), 1),
                1.0 / (1.0 + 2. * f.timeRevert / f.bft()),
                1.0 / (1.0 + 2. * f.timeDecel / f.bft())
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{4, 1, 1.95, 10, 2, 4, 2, 2, 2};
    }
}
