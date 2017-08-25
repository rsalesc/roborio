package rsalesc.roborio.movement.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;

import static rsalesc.roborio.movement.strategies.Slices.*;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public class SlicingAdaptiveStrategy extends MultipleSlicingStrategy {
    @Override
    public double[][][] getSlices() {
        return new double[][][]{
                {EMPTY, LAT_VEL_S, ADV_VEL_P, ESCAPE_P, EMPTY, ACCEL, EMPTY, D10_P},
                {EMPTY, LAT_VEL_S, EMPTY, ESCAPE_S, ESCAPE_P, ACCEL, RUN, D10_P},
                {EMPTY, LAT_VEL_S, ADV_VEL_P, EMPTY, ESCAPE_S, ACCEL_S, RUN_P, EMPTY},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, ESCAPE_S, EMPTY, ACCEL, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, ESCAPE_S, ACCEL, EMPTY, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL_S, ESCAPE, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, ACCEL, RUN_P, EMPTY},
                {EMPTY, LAT_VEL_P, ADV_VEL, ESCAPE, EMPTY, ACCEL_P, EMPTY, D10_P},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, ESCAPE_P, EMPTY, EMPTY, D10},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE_S, EMPTY, ACCEL_P, RUN_S, D10_P},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE_S, ESCAPE_S, ACCEL, RUN_P, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL, ESCAPE, EMPTY, ACCEL_S, RUN, D10_S},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, EMPTY, EMPTY, D10_P},
                {EMPTY, LAT_VEL_P, ADV_VEL_S, ESCAPE_P, ESCAPE_P, EMPTY, RUN_P, D10_P},
                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, EMPTY, EMPTY, D10},
                {EMPTY, LAT_VEL_P, ADV_VEL, ESCAPE_S, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_P, ADV_VEL, ESCAPE, EMPTY, EMPTY, RUN_P, D10_S},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_S, ESCAPE, ACCEL_S, RUN_P, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, ACCEL_P, RUN_P, D10},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, ACCEL_P, RUN, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_P, ESCAPE_S, ACCEL_P, RUN, D10},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE_P, ESCAPE, ACCEL_P, EMPTY, D10_S},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE, EMPTY, ACCEL, RUN_P, EMPTY},
                {EMPTY, LAT_VEL, EMPTY, ESCAPE, EMPTY, ACCEL, EMPTY, D10_P},
                {EMPTY, LAT_VEL, ADV_VEL_S, ESCAPE_S, ESCAPE_S, ACCEL, RUN_P, D10_S},
                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, EMPTY, RUN_P, EMPTY},
                {EMPTY, EMPTY, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY, EMPTY},
                {EMPTY, EMPTY, EMPTY, ESCAPE, ESCAPE_S, ACCEL, RUN_S, EMPTY},
                {EMPTY, EMPTY, EMPTY, ESCAPE, ESCAPE_S, EMPTY, RUN, D10},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL, RUN_P, D10_S},
                {EMPTY, EMPTY, ADV_VEL_P, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {EMPTY, EMPTY, ADV_VEL_P, ESCAPE_S, ESCAPE_P, EMPTY, RUN_S, D10},
                {BFT_S, LAT_VEL_P, ADV_VEL, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT_S, LAT_VEL_P, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, D10_P},
                {BFT_S, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT_S, LAT_VEL_S, EMPTY, ESCAPE_S, ESCAPE, ACCEL_P, EMPTY, D10},
                {BFT_S, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, ACCEL, RUN, D10_P},
                {BFT_S, EMPTY, EMPTY, ESCAPE_S, EMPTY, ACCEL, RUN_P, EMPTY},
                {BFT_S, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT_S, EMPTY, EMPTY, ESCAPE_P, ESCAPE_P, ACCEL_P, RUN_P, D10},
                {BFT_S, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {BFT_P, LAT_VEL, ADV_VEL, EMPTY, EMPTY, ACCEL, EMPTY, D10_S},
                {BFT_P, EMPTY, EMPTY, ESCAPE_P, EMPTY, ACCEL, EMPTY, EMPTY},
                {BFT_P, LAT_VEL_P, ADV_VEL_S, ESCAPE_P, ESCAPE_P, ACCEL_P, RUN_P, EMPTY},
                {BFT_P, LAT_VEL, EMPTY, ESCAPE, EMPTY, ACCEL_S, RUN_P, D10_S},
                {BFT_P, LAT_VEL, ADV_VEL_P, ESCAPE_S, EMPTY, EMPTY, RUN, D10},
                {BFT_P, EMPTY, ADV_VEL_P, ESCAPE_P, EMPTY, ACCEL, EMPTY, D10_S},
                {BFT_P, LAT_VEL_P, EMPTY, EMPTY, ESCAPE, EMPTY, RUN, D10_S},
                {BFT_P, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, ACCEL_S, EMPTY, D10},
                {BFT, LAT_VEL_S, EMPTY, EMPTY, ESCAPE, EMPTY, EMPTY, D10},
                {BFT, LAT_VEL_S, ADV_VEL, ESCAPE, EMPTY, ACCEL_S, RUN, D10_S},
                {BFT, LAT_VEL_P, EMPTY, ESCAPE_S, ESCAPE, ACCEL_S, EMPTY, EMPTY},
                {BFT, LAT_VEL_P, EMPTY, ESCAPE_P, ESCAPE_P, ACCEL_P, EMPTY, D10_S},
                {BFT, LAT_VEL_P, EMPTY, ESCAPE_S, EMPTY, ACCEL_S, RUN_P, EMPTY},
                {BFT, LAT_VEL, EMPTY, ESCAPE_S, EMPTY, ACCEL_P, EMPTY, EMPTY},
                {BFT, LAT_VEL, ADV_VEL_S, ESCAPE, EMPTY, ACCEL_S, EMPTY, EMPTY},
                {BFT, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, D10_S},
                {BFT, EMPTY, EMPTY, ESCAPE, ESCAPE_S, ACCEL, RUN_S, EMPTY},
                {BFT, EMPTY, ADV_VEL, ESCAPE, ESCAPE_P, ACCEL_S, EMPTY, D10_P}
        };
//        return new double[][][]{
//                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, ESCAPE_P, ACCEL_S, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_S, ADV_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, ESCAPE_S, ACCEL_S, RUN_P, EMPTY},
//                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, ESCAPE_P, ACCEL, RUN, EMPTY},
//                {EMPTY, LAT_VEL_S, EMPTY, EMPTY, EMPTY, ACCEL, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_S, EMPTY, ESCAPE, EMPTY, ACCEL_S, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, ESCAPE_S, ACCEL_S, EMPTY, D10},
//                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, EMPTY},
//                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE_S, EMPTY, EMPTY, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, ESCAPE_P, EMPTY, EMPTY, D10_S},
//                {EMPTY, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, D10_S},
//                {EMPTY, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, EMPTY, EMPTY, D10},
//                {EMPTY, LAT_VEL, EMPTY, ESCAPE_P, ESCAPE_S, ACCEL_S, RUN_P, D10},
//                {EMPTY, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, ACCEL_P, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL, EMPTY, EMPTY, ESCAPE_P, ACCEL_P, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
//                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, EMPTY, EMPTY, D10_S},
//                {EMPTY, LAT_VEL, ADV_VEL_S, EMPTY, EMPTY, ACCEL_P, EMPTY, D10},
//                {EMPTY, LAT_VEL, ADV_VEL_P, EMPTY, EMPTY, ACCEL_S, EMPTY, EMPTY},
//                {EMPTY, EMPTY, EMPTY, ESCAPE, EMPTY, EMPTY, EMPTY, D10_S},
//                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, RUN_S, D10},
//                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
//                {BFT_S, LAT_VEL_S, EMPTY, EMPTY, EMPTY, ACCEL_S, EMPTY, EMPTY},
//                {BFT_S, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL, EMPTY, EMPTY},
//                {BFT_S, LAT_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
//                {BFT_S, LAT_VEL, EMPTY, ESCAPE, EMPTY, ACCEL, RUN_P, EMPTY},
//                {BFT_S, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, EMPTY, EMPTY, EMPTY},
//                {BFT_S, LAT_VEL_S, EMPTY, ESCAPE, EMPTY, ACCEL, EMPTY, D10_S},
//                {BFT_P, LAT_VEL, ADV_VEL_S, ESCAPE_P, EMPTY, ACCEL_S, EMPTY, EMPTY},
//                {BFT_P, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, EMPTY, EMPTY, D10_S},
//                {BFT_P, LAT_VEL_P, EMPTY, EMPTY, ESCAPE_S, EMPTY, EMPTY, D10_S},
//                {BFT_P, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, ACCEL_S, EMPTY, EMPTY},
//                {BFT_P, LAT_VEL_P, EMPTY, ESCAPE, ESCAPE, EMPTY, EMPTY, EMPTY},
//                {BFT_P, LAT_VEL_P, ADV_VEL_P, EMPTY, EMPTY, EMPTY, EMPTY, D10},
//                {BFT_P, LAT_VEL, EMPTY, EMPTY, ESCAPE_S, ACCEL, EMPTY, EMPTY},
//                {BFT_P, LAT_VEL, EMPTY, EMPTY, ESCAPE, ACCEL, EMPTY, D10},
//                {BFT_P, EMPTY, ADV_VEL_S, ESCAPE, ESCAPE, EMPTY, RUN_S, D10_P},
//                {BFT, LAT_VEL_S, ADV_VEL, EMPTY, ESCAPE_P, EMPTY, EMPTY, EMPTY},
//                {BFT, LAT_VEL_P, EMPTY, ESCAPE_P, EMPTY, EMPTY, EMPTY, EMPTY}
//        };

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
