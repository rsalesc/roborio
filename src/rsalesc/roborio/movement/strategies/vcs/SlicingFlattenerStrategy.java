package rsalesc.roborio.movement.strategies.vcs;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;

import static rsalesc.roborio.movement.strategies.Slices.*;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public class SlicingFlattenerStrategy extends MultipleSlicingStrategy {
    @Override
    public double[][][] getSlices() {
        return new double[][][]{
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, ACCEL_S, EMPTY, D10},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, ESCAPE_S, ACCEL_S, RUN, D10_P},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, ACCEL_P, RUN, EMPTY},
                {EMPTY, LAT_VEL, ADV_VEL, EMPTY, EMPTY, EMPTY, RUN, D10_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL_P, EMPTY, D10_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL, EMPTY, EMPTY},
                {EMPTY, EMPTY, ADV_VEL_S, EMPTY, ESCAPE_P, EMPTY, EMPTY, D10_S},
                {BFT_S, EMPTY, ADV_VEL_P, ESCAPE_S, ESCAPE, EMPTY, RUN_P, D10},
                {BFT_P, LAT_VEL, ADV_VEL, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, ACCEL_P, EMPTY, D10},
                {BFT, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {BFT, EMPTY, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, EMPTY}
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
