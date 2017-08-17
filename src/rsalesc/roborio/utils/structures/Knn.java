package rsalesc.roborio.utils.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;
import rsalesc.roborio.utils.geo.Range;

import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public abstract class Knn<T> {
    private double scanWeight = 1.0;
    private int defaultK;
    private double defaultRatio;
    private Strategy strategy;
    private boolean built = false;
    private Condition condition = null;

    public Strategy getStrategy() {
        return strategy;
    }

    public Knn<T> setStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public Knn<T> setK(int K) {
        this.defaultK = K;
        return this;
    }

    public Knn<T> setRatio(double ratio) {
        this.defaultRatio = ratio;
        return this;
    }

    public Knn<T> setCondition(Condition condition) {
        this.condition = condition;
        return this;
    }

    public Knn<T> setScanWeight(double weight) {
        this.scanWeight = weight;
        return this;
    }

    public double getScanWeight() {
        return this.scanWeight;
    }

    protected abstract void buildStructure();
    public abstract int size();
    public abstract void add(double[] point, T payload);
    public abstract List<Entry<T>> query(double[] point, int K, double alpha);

    public boolean isEnabled(Object o) {
        if(condition == null)
            return true;
        return condition.test(o);
    }

    public boolean isBuilt() {
        return this.built;
    }

    public Knn<T> build() {
        buildStructure();
        built = true;
        return this;
    }

    public void add(TargetingLog f, T payload) {
        add(getStrategy().getQuery(f), payload);
    }

    public List<Entry<T>> query(double[] point) {
        return query(point, Math.min(defaultK, Math.max(1, (int) Math.ceil(size() * defaultRatio))), 1.0);
    }

    public List<Entry<T>> query(TargetingLog f) {
        return query(getStrategy().getQuery(f));
    }

    protected Entry<T> makeEntry(double distance, T payload) {
        return new Entry<T>(scanWeight, distance, payload);
    }

    public static class Entry<T> implements Comparable<Entry<T>> {
        public final double weight;
        public final double distance;
        public final T payload;

        public Entry(double weight, double distance, T payload) {
            this.weight = weight;
            this.distance = distance;
            this.payload = payload;
        }

        @Override
        public int compareTo(Entry<T> o) {
            return (int) Math.signum(distance - o.distance);
        }
    }

    private abstract class Condition {
        public abstract boolean test(Object o);
    }

    private class HitCondition {
        Range limits;

        public HitCondition(double min, double max) {
            limits = new Range(min, max);
        }

        public HitCondition(Range limits) {
            this(limits.min, limits.max);
        }

        public boolean test(Object o) {
            Range range = (Range) o;
            return range.intersect(limits).getLength() > R.EPSILON;
        }
    }
}
