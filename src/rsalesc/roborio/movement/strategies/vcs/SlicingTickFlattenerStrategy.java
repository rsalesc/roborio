package rsalesc.roborio.movement.strategies.vcs;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;

import static rsalesc.roborio.movement.strategies.Slices.*;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class SlicingTickFlattenerStrategy extends MultipleSlicingStrategy {
    @Override
    public double[][][] getSlices() {
        return new double[][][]{
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, ESCAPE, ACCEL, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE_S, EMPTY, ACCEL_P, RUN_P, D10_P},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, ACCEL, RUN, EMPTY},
                {EMPTY, EMPTY, EMPTY, ESCAPE_S, ESCAPE_S, ACCEL, RUN, EMPTY},
                {EMPTY, EMPTY, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, EMPTY, EMPTY, ESCAPE, EMPTY, ACCEL_P, RUN_P, D10_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, ESCAPE_S, EMPTY, RUN_P, EMPTY},
                {EMPTY, EMPTY, EMPTY, EMPTY, ESCAPE_P, ACCEL_P, RUN_S, EMPTY},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {EMPTY, EMPTY, ADV_VEL_S, ESCAPE, ESCAPE, ACCEL_P, EMPTY, D10_P},
                {EMPTY, EMPTY, ADV_VEL_S, ESCAPE_S, ESCAPE_P, ACCEL_P, EMPTY, EMPTY},
                {EMPTY, EMPTY, ADV_VEL_S, ESCAPE, EMPTY, EMPTY, EMPTY, D10_P},
                {BFT_P, EMPTY, ADV_VEL_P, ESCAPE_P, ESCAPE_P, ACCEL_S, EMPTY, EMPTY},
                {BFT_P, LAT_VEL_P, EMPTY, EMPTY, ESCAPE_S, ACCEL_S, EMPTY, D10},
                {BFT_P, EMPTY, EMPTY, EMPTY, ESCAPE_P, ACCEL_S, EMPTY, EMPTY},
                {BFT, LAT_VEL_S, EMPTY, ESCAPE_S, EMPTY, EMPTY, RUN, D10_S},
                {BFT, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL, EMPTY, EMPTY}
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
