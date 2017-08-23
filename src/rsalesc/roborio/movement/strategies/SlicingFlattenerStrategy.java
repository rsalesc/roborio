package rsalesc.roborio.movement.strategies;

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
                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_S, EMPTY, ESCAPE_P, EMPTY, ACCEL_P, RUN_P, EMPTY},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, EMPTY, RUN, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, EMPTY, RUN, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, D10},
                {EMPTY, EMPTY, EMPTY, ESCAPE_P, EMPTY, ACCEL_S, EMPTY, D10},
                {EMPTY, EMPTY, EMPTY, EMPTY, ESCAPE, ACCEL_S, EMPTY, EMPTY},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, D10_P},
                {EMPTY, EMPTY, ADV_VEL_S, EMPTY, EMPTY, EMPTY, RUN_P, EMPTY},
                {BFT_S, LAT_VEL_P, ADV_VEL, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY},
                {BFT_S, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT_S, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT_S, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, ACCEL, EMPTY, EMPTY},
                {BFT_S, LAT_VEL, ADV_VEL, ESCAPE_P, EMPTY, ACCEL, EMPTY, D10_P},
                {BFT_S, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, D10},
                {BFT_P, LAT_VEL_P, ADV_VEL, ESCAPE_S, EMPTY, ACCEL, RUN, EMPTY},
                {BFT_P, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT_P, EMPTY, ADV_VEL_P, EMPTY, ESCAPE_P, ACCEL_S, RUN_S, EMPTY},
                {BFT_P, LAT_VEL_S, EMPTY, EMPTY, EMPTY, ACCEL_P, RUN_S, EMPTY},
                {BFT_P, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT_P, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT, LAT_VEL_S, EMPTY, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT, LAT_VEL_P, EMPTY, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT, LAT_VEL_P, ADV_VEL_P, ESCAPE, EMPTY, ACCEL, EMPTY, EMPTY},
                {BFT, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {BFT, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, EMPTY, RUN, EMPTY},
                {BFT, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, EMPTY}
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
