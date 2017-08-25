package rsalesc.roborio.utils.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.waves.BreakType;
import rsalesc.roborio.utils.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public class KnnSet<T> {
    private List<Knn<T>> knns;
    private DistanceWeighter<T> weighter;

    public KnnSet() {
        knns = new ArrayList<>();
    }

    public KnnSet<T> setDistanceWeighter(DistanceWeighter<T> weighter) {
        this.weighter = weighter;
        return this;
    }

    public KnnSet<T> add(Knn<T> knn) {
        if(!knn.isBuilt())
            knn.build();
        knns.add(knn);
        return this;
    }

    public void add(TargetingLog f, T payload) {
        for(Knn<T> knn : knns)
            knn.add(f, payload);
    }

    public void add(TargetingLog f, T payload, BreakType type) {
        for(Knn<T> knn : knns) {
            if(type == BreakType.BULLET_HIT && knn.logsOnHit())
                knn.add(f, payload);
            else if(type == BreakType.BULLET_BREAK && knn.logsOnBreak())
                knn.add(f, payload);
            else if(type == BreakType.VIRTUAL_BREAK && knn.logsOnVirtual())
                knn.add(f, payload);
        }
    }

    public List<Knn.Entry<T>> query(TargetingLog f) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            res.addAll(knn.query(f));
        }

        Collections.sort(res);
        if(weighter != null)
            res = weighter.getWeightedEntries(res);
        
        return res;
    }

    public List<Knn.Entry<T>> query(TargetingLog f, Object o) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            if(knn.isEnabled(o))
                res.addAll(knn.query(f));
        }

        Collections.sort(res);
        if(weighter != null)
            res = weighter.getWeightedEntries(res);

        return res;
    }

    public List<Knn<T>> getKnns() {
        return knns;
    }

    public static abstract class DistanceWeighter<T> {
        public abstract List<Knn.Entry<T>> getWeightedEntries(List<Knn.Entry<T>> entries);
    }

    public static class InverseDistanceWeighter<T> extends DistanceWeighter<T> {
        private double ratio;
        public InverseDistanceWeighter() {
            this(1.0);
        }

        public InverseDistanceWeighter(double ratio) {
            this.ratio = ratio;
        }

        @Override
        public List<Knn.Entry<T>> getWeightedEntries(List<Knn.Entry<T>> entries) {
            List<Knn.Entry<T>> res = new ArrayList<>();
            for(Knn.Entry<T> entry : entries) {
                res.add(new Knn.Entry<>(entry.weight / Math.pow(entry.distance + 1e-10, ratio),
                        entry.distance, entry.payload));
            }

            return res;
        }
    }

    public static class GaussDistanceWeighter<T> extends DistanceWeighter<T> {
        private double ratio;

        public GaussDistanceWeighter() {
            this(1.0);
        }

        public GaussDistanceWeighter(double ratio) {
            this.ratio = ratio;
        }

        public List<Knn.Entry<T>> getWeightedEntries(List<Knn.Entry<T>> entries) {
            double sum = 1e-9;
            for(Knn.Entry<T> entry : entries) {
                sum += entry.distance;
            }

            double invAvg = entries.size() / sum;

            List<Knn.Entry<T>> res = new ArrayList<>();

            for(Knn.Entry<T> entry : entries) {
                res.add(new Knn.Entry<T>(entry.weight  * R.gaussKernel(entry.distance * invAvg * ratio),
                        entry.distance, entry.payload));
            }

            return res;
        }
    }
}
