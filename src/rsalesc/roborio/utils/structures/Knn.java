package rsalesc.roborio.utils.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Strategy;
import rsalesc.roborio.utils.geo.Range;

import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public abstract class Knn<T> {
    private boolean hasSpecificLog = false;
    private boolean loggingHit = true;
    private boolean loggingBreak = true;
    private boolean loggingVirtual = false;

    private double scanWeight = 1.0;
    private int defaultK;
    private double defaultRatio = 1.0;
    private Strategy strategy;
    private boolean built = false;
    private ParametrizedCondition parametrizedCondition = null;

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

    public Knn<T> setCondition(ParametrizedCondition parametrizedCondition) {
        this.parametrizedCondition = parametrizedCondition;
        return this;
    }

    public Knn<T> setScanWeight(double weight) {
        this.scanWeight = weight;
        return this;
    }

    public double getScanWeight() {
        return this.scanWeight;
    }

    private void setupLogging() {
        if(!hasSpecificLog) {
            loggingHit = false;
            loggingBreak = false;
            loggingVirtual = false;
        }
        hasSpecificLog = true;
    }

    public Knn<T> logsHit() {
        setupLogging();
        loggingHit = true;
        return this;
    }

    public Knn<T> logsBreak() {
        setupLogging();
        loggingBreak = true;
        return this;
    }

    public Knn<T> logsVirtual() {
        setupLogging();
        loggingVirtual = true;
        return this;
    }

    public boolean logsOnHit() {
        return loggingHit;
    }

    public boolean logsOnBreak() {
        return loggingBreak;
    }

    public boolean logsOnVirtual() {
        return loggingVirtual;
    }

    protected abstract void buildStructure();
    public abstract int size();
    public abstract void add(double[] point, T payload);
    public abstract List<Entry<T>> query(double[] point, int K, double alpha);

    public boolean isEnabled(Object o) {
        if(parametrizedCondition == null)
            return true;
        return parametrizedCondition.test(o);
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

    public static abstract class ParametrizedCondition {
        public abstract boolean test(Object o);
    }

    public static class HitCondition extends ParametrizedCondition {
        Range limits;
        int rounds;

        public HitCondition(double min, double max, int rounds) {
            limits = new Range(min, max);
            this.rounds = rounds;
        }

        public HitCondition(Range limits, int rounds) {
            this(limits.min, limits.max, rounds);
        }

        public boolean test(Object o) {
            HitLeastCondition range = (HitLeastCondition) o;
            if(limits.isNearlyContained(range.limits.min) && range.rounds >= rounds)
                return true;
            else
                return false;
        }
    }

    public static class HitLeastCondition extends HitCondition {
        public HitLeastCondition(double min, int rounds) {
            super(min, Double.MAX_VALUE, rounds);
        }
    }
}
