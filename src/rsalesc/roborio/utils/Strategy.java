package rsalesc.roborio.utils;

import rsalesc.roborio.gunning.utils.TargetingLog;

import java.util.Arrays;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public abstract class Strategy {
    public abstract double[] getQuery(TargetingLog f);
    public abstract double[] getWeights();

    public static double[] unitaryWeight(int size) {
        double[] res = new double[size];
        Arrays.fill(res, 1);
        return res;
    }
}
