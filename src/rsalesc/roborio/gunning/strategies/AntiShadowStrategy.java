package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiShadowStrategy extends Strategy {
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
                1.0 / (0.5 * f.timeDecel + 1),
                1.0 / (0.5 * f.timeRevert + 1),
                (R.constrain(-1, f.coveredLast20, +1) + 1) * 0.5,
                Math.min(f.revertLast20 / 20, 1),
                Math.pow(0.6 * f.bulletsFired, 1.15)
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{2, 5, 1, 4, 3, 1, 4, 4, 2, 3, 1.5, 6, 3.5};
    }
}
