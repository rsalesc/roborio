package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class GeneralPurposeStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.min(f.bft() / 81., 1),
                f.lateralVelocity / 8.,
                (f.advancingVelocity / 16. + 1) / 2,
                Math.min(f.positiveEscape / 400., 1),
                Math.min(f.negativeEscape / 400., 1),
                (f.accel + 1) / 2.0,
                f.aiming ? 0 : Math.min((f.gunHeat + 0.1) / 1.5, 1.0),
                1.0 / (0.5 * f.timeRevert + 1)
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{5, 4, 1, 4, 1, 2, 2, 2};
    }
}
