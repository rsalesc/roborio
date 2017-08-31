package rsalesc.roborio.movement.strategies.dc;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 29/08/17.
 */
public class PreciseFlatteningStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.max(f.bft() / 80, 1),
                Math.max(f.lateralVelocity / 8, 1),
                Math.max((f.advancingVelocity + 8) / 16.0, 1),
                (f.accel + 1) * 0.5,
                R.constrain(0, f.getPreciseMea().max / f.getMea(), 1),
                R.constrain(0, -f.getPreciseMea().min / f.getMea(), 1),
                1.0 / (1.0 + 2 * f.timeDecel),
                1.0 / (1.0 + 2 * f.timeRevert),
                Math.max(f.displaceLast10 / 80, 1),
                Math.max(f.displaceLast20 / 160, 1),
                Math.max(f.displaceLast40 / 320, 1),
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{3, 4, 5, 1, 4, 1.5, 3, 2, 2, 2, 1};
    }
}