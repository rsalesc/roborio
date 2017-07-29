package roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class GaussianSmoothing extends KernelSmoothing {
    private static final double     GAUSS_RATIO = 1.0 / (Math.sqrt(Math.PI * 2));
    public GaussianSmoothing(double bandwidth) {
        super(bandwidth);
    }

    @Override
    public double evaluateKernel(double x, double x0) {
        return Math.exp(-0.5 * sqr((x - x0) / bandwidth)) * GAUSS_RATIO;
    }

    private double sqr(double v) {
        return v*v;
    }

    @Override
    public double binToX(int bin) {
        return bin;
    }

    @Override
    public int XToBin(double x) {
        return (int) x;
    }


}
