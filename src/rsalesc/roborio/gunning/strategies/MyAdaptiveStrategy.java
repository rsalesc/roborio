package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;

/**
 * Created by Roberto Sales on 16/08/17.
 */
public class MyAdaptiveStrategy extends Strategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.min(f.distance / 880., 1), // was bft before
                R.sin(f.relativeHeading), // was 4
                (R.cos(f.relativeHeading) + 1) / 2.,
                Math.abs(f.velocity) / 8.,
                Math.min(f.positiveEscape / 400., 1),
                Math.min(f.negativeEscape / 400., 1),
                (f.accel + 1) / 2.0, // weight was 1
                1.0 / (1.0 + 0.1 * f.timeRevert), // had no bft
                1.0 / (1.0 + 0.1 * f.timeDecel),
                (f.lastMissGF + 1) / 2.,
                Math.max(f.displaceLast10 / 80., 1)
//                f.aiming ? 0 : Math.min((f.gunHeat + 0.1) / 1.5, 1.0),
        };
    }

    @Override
    public double[] getWeights() {
        return new double[]{2, 6, 1, 3, 4, 2, 2, 1.5, 2, 1, 2, /*1*/};
    }
}

//    @Override
//    public double[] getQuery(TargetingLog f) {
//        return new double[]{
//                Math.min(f.distance / 880., 1), // was bft before
//                R.sin(f.relativeHeading), // was 4
//                (R.cos(f.relativeHeading) + 1) / 2.,
//                Math.abs(f.velocity) / 8.,
//                Math.min(f.positiveEscape / 400., 1),
//                Math.min(f.negativeEscape / 400., 1),
//                (f.accel + 1) / 2.0, // weight was 1
//                f.aiming ? 0 : Math.min((f.gunHeat + 0.1) / 1.5, 1.0),
//                1.0 / (1.0 + 0.2 * f.timeRevert), // had no bft
//                1.0 / (1.0 + 0.2 * f.timeDecel),
//                (f.lastMissGF + 1) / 2.
//        };
//    }
//
//    @Override
//    public double[] getWeights() {
//        return new double[]{2, 5, 1, 3, 4, 2, 1.5, 2, 1.5, 2, 2};
//    }