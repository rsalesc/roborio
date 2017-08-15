package rsalesc.roborio.gunning.virtual;

import rsalesc.roborio.gunning.VirtualGunArray;

import java.util.HashMap;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class HitRateScoring extends GunScoring {
    HashMap<Integer, Integer> shotsFired;
    HashMap<Integer, Double> stats;

    public HitRateScoring() {
        stats = new HashMap<>();
        shotsFired = new HashMap<>();
    }

    private double get(int i) {
        Double res = stats.get(i);
        if(res == null)
            return 0;
        return res;
    }

    @Override
    public void fire(int index, double power) {
        shotsFired.put(index, shotsFired.getOrDefault(index, 0) + 1);
    }

    @Override
    public void log(int index, double alpha, double power) {
        stats.put(index, get(index) + alpha);
    }

    @Override
    public double[] evaluate(VirtualGunArray array) {
        double[] res = new double[array.length];
        for(int i = 0; i < array.length; i++) {
            res[i] = get(i) / shotsFired.getOrDefault(i, 1);
        }

        return res;
    }
}