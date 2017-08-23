package rsalesc.roborio.utils;

import rsalesc.roborio.gunning.utils.TargetingLog;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class DecayableStrategy extends Strategy {
    private double alpha;
    private double exp;
    private Strategy strategy;

    public DecayableStrategy(double alpha, double exp) {
        this.alpha = alpha;
        this.exp = exp;
    }

    public DecayableStrategy setStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    @Override
    public double[] getQuery(TargetingLog f) {
        double[] query = strategy.getQuery(f);
        double[] res = new double[query.length + 1];
        for(int i = 0; i < query.length; i++) res[i] = query[i];
        res[query.length] = Math.pow(alpha * f.bulletsFired, exp);
        return res;
    }

    @Override
    public double[] getWeights() {
        double[] w = strategy.getWeights();
        double[] res = new double[w.length + 1];
        for(int i = 0; i < w.length; i++) res[i] = w[i];
        res[w.length] = 1.0;
        return res;
    }
}
