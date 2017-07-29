package roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public abstract class Smoothing {
    public abstract double[] smooth(double[] input);
    public abstract double binToX(int bin);
    public abstract int XToBin(double x);
}
