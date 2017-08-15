package rsalesc.roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class UniformSmoothing extends KernelSmoothing {

    public UniformSmoothing(double bandwidth) {
        super(bandwidth);
    }

    @Override
    public double evaluateKernel(int diff) {
        double parameter = (diff) / bandwidth;
        if(Math.abs(parameter) > 1)
            return 0.0;
        return parameter * 0.5;
    }
}
