package rsalesc.roborio.movement.strategies.dc;

import rsalesc.roborio.gunning.utils.TargetingLog;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class UnsegmentedStrategy extends rsalesc.roborio.utils.Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.max(f.bft() / 81, 1),
                Math.max(f.lateralVelocity / 8., 1),
                Math.max((f.advancingVelocity + 8) / 16, 1),
                Math.max((f.accel + 1) / 2, 1)
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{1, 1, 1, 1};
    }
}
