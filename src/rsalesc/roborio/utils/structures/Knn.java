package rsalesc.roborio.utils.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;
import rsalesc.roborio.utils.geo.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public abstract class Knn<T> {
    private boolean hasSpecificLog = false;
    private boolean loggingHit = true;
    private boolean loggingBreak = false;
    private boolean loggingVirtual = false;

    private Double decayDepth = null;
    private double scanWeight = 1.0;
    private int defaultK;
    private double defaultRatio = 1.0;
    private Strategy strategy;
    private boolean built = false;
    private ParametrizedCondition parametrizedCondition = null;
    private DistanceWeighter weighter;

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

    public Knn<T> setDecayDepth(double r) {
        decayDepth = 1.0 - 1.0 / (1.0 + r);
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

    public void mutate(ConditionMutation mutation) {
        if(parametrizedCondition != null)
            parametrizedCondition.mutate(mutation);
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
        List<Entry<T>> res =
                query(point, Math.min(defaultK, Math.max(1, (int) Math.ceil(size() * defaultRatio))), 1.0);
        if(decayDepth == null)
            return res;

        Collections.sort(res);
        if(weighter != null)
            res = weighter.getWeightedEntries(res);

        double multiplier = 1.0;
        for(Entry<T> entry : res) {
            entry.weight *= multiplier;
            multiplier *= decayDepth;
        }

        return res;
    }

    public List<Entry<T>> query(TargetingLog f) {
        return query(getStrategy().getQuery(f));
    }

    protected Entry<T> makeEntry(double distance, T payload) {
        return new Entry<T>(scanWeight, distance, payload);
    }

    public Knn<T> setDistanceWeighter(DistanceWeighter weighter) {
        this.weighter = weighter;
        return this;
    }

    public static class Entry<T> implements Comparable<Entry<T>> {
        public double weight;
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
        public abstract void mutate(ConditionMutation mutation);
    }

    public static class OrCondition extends ParametrizedCondition {
        ArrayList<ParametrizedCondition> conditions = new ArrayList<>();

        public OrCondition add(ParametrizedCondition condition) {
            conditions.add(condition);
            return this;
        }

        @Override
        public boolean test(Object o) {
            boolean res = false;

            // cant return immediately because of mutation
            for(ParametrizedCondition condition : conditions) {
                res = res || condition.test(o);
            }

            return res;
        }

        @Override
        public void mutate(ConditionMutation mutation) {
            for(ParametrizedCondition condition : conditions)
                condition.mutate(mutation);
        }
    }

    public static class AndCondition extends ParametrizedCondition {
        ArrayList<ParametrizedCondition> conditions = new ArrayList<>();

        public AndCondition add(ParametrizedCondition condition) {
            conditions.add(condition);
            return this;
        }

        @Override
        public boolean test(Object o) {
            boolean res = true;

            // cant return immediately because of mutation
            for(ParametrizedCondition condition : conditions) {
                res = res && condition.test(o);
            }

            return res;
        }

        @Override
        public void mutate(ConditionMutation mutation) {
            for(ParametrizedCondition condition : conditions)
                condition.mutate(mutation);
        }
    }

    public static class NotCondition extends ParametrizedCondition {
        ParametrizedCondition condition;

        public NotCondition(ParametrizedCondition condition) {
            this.condition = condition;
        }

        @Override
        public boolean test(Object o) {
            return !condition.test(o);
        }

        @Override
        public void mutate(ConditionMutation mutation) {
            condition.mutate(mutation);
        }
    }

    public static class ConditionMutation {
        public final long time;
        public final int round;

        public ConditionMutation(long time, int round) {
            this.time = time;
            this.round = round;
        }
    }

    public static class HitCondition extends ParametrizedCondition {
        protected Range limits;
        protected int rounds;

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

        @Override
        public void mutate(ConditionMutation mutation) {}
    }

    public static class HitLeastCondition extends HitCondition {
        public HitLeastCondition(double min, int rounds) {
            super(min, Double.MAX_VALUE, rounds);
        }
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
