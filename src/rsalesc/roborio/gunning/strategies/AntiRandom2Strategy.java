package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiRandom2Strategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.min(f.bft() / 80, 1),
                R.constrain(0, f.bulletPower / 3, 1),
                R.sin(f.relativeHeading), // was 4
                (R.cos(f.relativeHeading) + 1) / 2.,
                Math.abs(f.velocity) / 8.,
                R.constrain(0, (f.accel + 1) / 2, 1),
                R.constrain(0, f.getPreciseMea().max / f.getMea(), 1),
                R.constrain(0, -f.getPreciseMea().min / f.getMea(), 1),
//                Math.min(f.positiveEscape / 400., 1),
//                Math.min(f.negativeEscape / 400., 1),
//                1.0 / (1.0 + 2. * f.timeRevert),
                1.0 / (1.0 + 2. * f.timeDecel),
                f.aiming ? 0 : f.heat()
        };
    }

    @Override
    public double[] getWeights() {
//        return new double[]{5, 0.5, 3.5, 3, 2, 4, 4, 2, 1.75, 1};
        return new double[]{5, 4, 4, 7, 1, 3, 4, 2, 4, 3};
    }
}
