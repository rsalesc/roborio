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
                Math.min(f.bft() / 81., 1),
                R.sin(f.relativeHeading),
                (R.cos(f.relativeHeading) + 1) / 2.,
                Math.abs(f.velocity) / 8.,
                Math.min(f.positiveEscape / 400., 1),
                Math.min(f.negativeEscape / 400., 1),
                (f.accel + 1) / 2.0,
                f.aiming ? 0 : Math.min((f.gunHeat + 0.1) / 1.5, 1.0),
                1.0 / (1.0 + 0.5 * f.timeRevert),
                1.0 / (1.0 + 0.5 * f.timeDecel),
                (f.lastMissGF + 1) / 2.
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{2, 4, 1, 3, 4, 2, 1, 2, 0, 2, 2};
    }
}
