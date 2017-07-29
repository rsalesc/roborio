package roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public abstract class KernelSmoothing extends Smoothing {
    protected double bandwidth;
    public KernelSmoothing(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public abstract double evaluateKernel(double x, double x0);

    @Override
    public double[] smooth(double[] input) {
        double[] output = new double[input.length];

        for(int i = 0; i < input.length; i++) {
            for(int j = 0; j < input.length; j++) {
                output[i] += input[j] * evaluateKernel(binToX(i), binToX(j));
            }
        }

        return output;
    }
}
