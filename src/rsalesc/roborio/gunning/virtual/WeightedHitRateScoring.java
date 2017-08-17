package rsalesc.roborio.gunning.virtual;

import rsalesc.roborio.gunning.VirtualGunArray;

import java.util.HashMap;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class WeightedHitRateScoring extends GunScoring {
    HashMap<Integer, Double> powerFired;
    HashMap<Integer, Double> stats;

    public WeightedHitRateScoring() {
        stats = new HashMap<>();
        powerFired = new HashMap<>();
    }

    private double get(int i) {
        Double res = stats.get(i);
        if(res == null)
            return 0;
        return res;
    }

    @Override
    public void fire(int index, double alpha, double power) {
        powerFired.put(index, powerFired.getOrDefault(index, 0.0) + alpha * power);
    }

    @Override
    public void log(int index, double alpha, double power) {
        stats.put(index, get(index) + alpha * power);
    }

    @Override
    public double[] evaluate(VirtualGunArray array) {
        double[] res = new double[array.length];
        for(int i = 0; i < array.length; i++) {
            res[i] = get(i) / powerFired.getOrDefault(i, 1.0);
        }

        return res;
    }
}
