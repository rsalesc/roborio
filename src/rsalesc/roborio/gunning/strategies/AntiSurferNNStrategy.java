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
                f.bft(),
                f.lateralVelocity,
                f.advancingVelocity,
                R.constrain(0, f.getPreciseMea().max / f.getMea(), 1.25),
                R.constrain(0, -f.getPreciseMea().min / f.getMea(), 1.25),
                f.accel,
                Math.max(f.timeDecel / f.bft(), 1.25),
                Math.max(f.timeRevert / f.bft(), 1.25),
                f.displaceLast10
        };
    }

    @Override
    public double[][] getSlices() {
        return new double[][]{
                SlicingStrategy.uniformSlicing(0, 85, 8),
                SlicingStrategy.uniformSlicing(0, 8, 9),
                SlicingStrategy.uniformSlicing(-8, 8, 6),
                SlicingStrategy.uniformSlicing(0, 1.25, 6),
                SlicingStrategy.uniformSlicing(0, 1.25, 3),
                SlicingStrategy.uniformSlicing(-1, +1, 8),
                SlicingStrategy.uniformSlicing(0, 1.25, 7),
                SlicingStrategy.uniformSlicing(0, 1.25, 7),
                SlicingStrategy.uniformSlicing(0, 80, 5)
        };
    }
}
