package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public abstract class SlicingStrategy extends Strategy {
    public abstract double[][] getSlices();

    @Override
    public double[] getWeights() {
        return Strategy.unitaryWeight(getSlices().length);
    }

    public static double[] uniformSlicing(double left, double right, int count) {
        double[] res = new double[count + 1];
        double step = (right - left) / count;
        res[0] = left;
        for(int i = 1; i <= count; i++)
            res[i] = res[i-1] + step;
        return res;
    }
}
