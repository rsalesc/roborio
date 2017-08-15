package rsalesc.roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class HarmonicSmoothing extends Smoothing {
    private double bandwidth;
    public HarmonicSmoothing(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    @Override
    public double[] smooth(double[] input) {
        double[] output = new double[input.length];
        for(int i = 0; i < input.length; i++) {
            for(int j = 0; j < input.length; j++) {
                output[i] += input[j] * (1.0 / (sqr((double)(j-i) / bandwidth) + 1.0));
            }
        }
        return output;
    }

    private double sqr(double v) {
        return v*v;
    }
}
