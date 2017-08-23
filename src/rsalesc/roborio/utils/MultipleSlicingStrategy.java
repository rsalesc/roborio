package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 23/08/17.
 */
public abstract class MultipleSlicingStrategy extends Strategy {
    public abstract double[][][] getSlices();

    @Override
    public double[] getWeights() {
        return Strategy.unitaryWeight(getSlices().length);
    }
}
