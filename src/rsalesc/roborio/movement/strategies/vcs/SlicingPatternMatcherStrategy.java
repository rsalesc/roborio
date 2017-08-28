package rsalesc.roborio.movement.strategies.vcs;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;

import static rsalesc.roborio.movement.strategies.Slices.*;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class SlicingPatternMatcherStrategy extends MultipleSlicingStrategy {
    @Override
    public double[][][] getSlices() {
        return new double[][][]{
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN, EMPTY},
                {EMPTY, LAT_VEL_S, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN_S, D10},
                {EMPTY, LAT_VEL_S, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN_P, D10},
                {EMPTY, LAT_VEL_S, ADV_VEL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, D10_S},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_P, EMPTY},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, ACCEL, EMPTY, D10_P},
                {EMPTY, LAT_VEL_S, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN, D10},
                {EMPTY, LAT_VEL_S, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN_S, EMPTY},
                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, ACCEL_P, EMPTY, D10_P},
                {EMPTY, LAT_VEL_S, ADV_VEL, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, D10},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, LAT_VEL_S, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL, EMPTY, EMPTY, ACCEL_P, EMPTY, D10_S},
                {EMPTY, LAT_VEL_P, ADV_VEL_P, EMPTY, EMPTY, ACCEL_S, RUN_S, EMPTY},
                {EMPTY, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_S, D10},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, RUN, D10},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_P, ADV_VEL_P, EMPTY, EMPTY, ACCEL_S, RUN_S, D10},
                {EMPTY, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_P, D10},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, D10},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, D10},
                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, D10_S},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN_P, EMPTY},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN_P, D10},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN, D10},
                {EMPTY, LAT_VEL, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN_S, D10},
                {EMPTY, LAT_VEL, ADV_VEL, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL, EMPTY, EMPTY, ACCEL_S, RUN_P, D10_S},
                {EMPTY, LAT_VEL, ADV_VEL, EMPTY, EMPTY, ACCEL_S, RUN_P, D10}
        };

    }

    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                f.bft(),
                f.lateralVelocity,
                f.advancingVelocity,
                f.positiveEscape,
                f.negativeEscape,
                f.accel,
                Math.max(f.run / f.bft(), 1),
                f.displaceLast10
        };
    }
}
