package roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class UniformSmoothing extends KernelSmoothing {

    public UniformSmoothing(double bandwidth) {
        super(bandwidth);
    }

    @Override
    public double evaluateKernel(double x, double x0) {
        double parameter = (x - x0) / bandwidth;
        if(Math.abs(parameter) > 1)
            return 0.0;
        return (x - x0) / bandwidth * 0.5;
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
