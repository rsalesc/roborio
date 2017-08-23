package rsalesc.roborio.movement.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class FlatteningStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.max(f.bft() / 80, 1),
                Math.max(f.lateralVelocity / 8, 1),
                Math.max((f.advancingVelocity + 8) / 16.0, 1),
                (f.accel + 1) * 0.5,
                Math.max(f.positiveEscape / 400, 1),
                Math.max(f.negativeEscape / 400, 1),
                Math.max((double) f.run / f.bft(), 1),
                Math.max(f.displaceLast10 / 80.0, 1)
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{3, 4, 5, 1, 4, 3, 3, 2};
    }
}
