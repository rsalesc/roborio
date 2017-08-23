package rsalesc.roborio.utils.nn;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public abstract class MLPRegularization {
    protected int outputSize;

    public MLPRegularization setOutputSize(int x) {
        outputSize = x;
        return this;
    }

    public abstract double getValue(double[][][] weights);
    public abstract double[][] getDerivative(double[][] w);
}
