package roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class NoSmoothing extends Smoothing {
    @Override
    public double[] smooth(double[] input) {
        return input;
    }
}
