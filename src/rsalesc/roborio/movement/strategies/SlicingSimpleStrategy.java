package rsalesc.roborio.movement.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;

import static rsalesc.roborio.movement.strategies.Slices.*;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public class SlicingSimpleStrategy extends MultipleSlicingStrategy {
    @Override
    public double[][][] getSlices() {
        return new double[][][]{
                {EMPTY, LAT_VEL_S, ADV_VEL_S, EMPTY, EMPTY, EMPTY},
                {EMPTY, LAT_VEL_S, ADV_VEL_S, ESCAPE_S, EMPTY, ACCEL_S},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, ACCEL},
                {EMPTY, LAT_VEL_P, ADV_VEL_P, EMPTY, EMPTY, ACCEL_S},
                {EMPTY, LAT_VEL_P, EMPTY, EMPTY, EMPTY, ACCEL},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_S},
                {EMPTY, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL_P},
                {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL},
                {EMPTY, EMPTY, ADV_VEL, ESCAPE, EMPTY, ACCEL_P},
                {BFT_S, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL_P},
                {BFT_S, EMPTY, EMPTY, EMPTY, EMPTY, ACCEL},
                {BFT_S, LAT_VEL_S, ADV_VEL_P, EMPTY, EMPTY, EMPTY},
                {BFT_S, LAT_VEL, EMPTY, ESCAPE_P, EMPTY, ACCEL_S},
                {BFT_S, LAT_VEL_P, EMPTY, ESCAPE, EMPTY, ACCEL_S},
                {BFT_S, LAT_VEL, EMPTY, EMPTY, EMPTY, EMPTY},
                {BFT_S, LAT_VEL_S, EMPTY, ESCAPE_P, EMPTY, ACCEL},
                {BFT_S, EMPTY, ADV_VEL_P, ESCAPE, EMPTY, EMPTY},
                {BFT_P, LAT_VEL_P, EMPTY, ESCAPE_P, EMPTY, EMPTY},
                {BFT_P, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_P},
                {BFT, LAT_VEL_S, ADV_VEL, ESCAPE_P, EMPTY, EMPTY},
                {BFT, LAT_VEL_P, ADV_VEL_S, EMPTY, EMPTY, ACCEL},
                {BFT, LAT_VEL, EMPTY, EMPTY, ESCAPE_P, EMPTY},
                {BFT, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_S},
                {BFT, LAT_VEL, EMPTY, EMPTY, EMPTY, ACCEL_S}
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
                f.accel
        };
    }
}
