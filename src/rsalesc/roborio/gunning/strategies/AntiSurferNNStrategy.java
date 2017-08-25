package rsalesc.roborio.gunning.strategies;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.SlicingStrategy;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public class AntiSurferNNStrategy extends SlicingStrategy {
    @Override
    public double[] getQuery(TargetingLog f) {
        return new double[]{
                Math.max(f.distance, 800),
                R.sin(f.relativeHeading),
                R.cos(f.relativeHeading),
                f.velocity,
                Math.max(f.positiveEscape, 400),
                Math.max(f.negativeEscape, 400),
                f.accel,
                1.0 / (1.0 + 0.1 * f.timeRevert),
                1.0 / (1.0 + 0.1 * f.timeDecel),
                f.lastMissGF
        };
    }

    @Override
    public double[][] getSlices() {
        return new double[][]{
                SlicingStrategy.uniformSlicing(0, 800, 11),
                SlicingStrategy.uniformSlicing(0, 1, 10),
                SlicingStrategy.uniformSlicing(-1, +1, 7),
                SlicingStrategy.uniformSlicing(0, 8, 9),
                SlicingStrategy.uniformSlicing(0, 600, 7),
                SlicingStrategy.uniformSlicing(0, 600, 4),
                SlicingStrategy.uniformSlicing(-1, +1, 5),
                SlicingStrategy.uniformSlicing(0, 1, 6),
                SlicingStrategy.uniformSlicing(0, 1, 8),
                SlicingStrategy.uniformSlicing(-1, +1, 5)
        };
    }
}
