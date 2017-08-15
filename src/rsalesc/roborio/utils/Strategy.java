package rsalesc.roborio.utils;

import rsalesc.roborio.gunning.utils.TargetingLog;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public abstract class Strategy {
    public abstract double[] getQuery(TargetingLog f);
    public abstract double[] getWeights();
}
