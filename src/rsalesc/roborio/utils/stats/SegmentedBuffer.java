package rsalesc.roborio.utils.stats;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.SlicingStrategy;
import rsalesc.roborio.utils.structures.Knn;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public class SegmentedBuffer {
    protected boolean built = false;

    private boolean hasSpecificLog = false;
    private boolean loggingHit = true;
    private boolean loggingBreak = true;
    private boolean loggingVirtual = false;

    protected boolean hasDepth = false;
    protected double rollingDepth = Double.POSITIVE_INFINITY;
    protected Segmentation<GuessFactorStats> segmentation;
    protected SlicingStrategy strategy;

    protected double weight = 1.0;
    protected Knn.ParametrizedCondition condition;

    public SegmentedBuffer setStrategy(SlicingStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public SegmentedBuffer setRollingDepth(double depth) {
        rollingDepth = depth;
        hasDepth = true;
        return this;
    }

    public SegmentedBuffer setWeight(double weight) {
        this.weight = weight;
        return this;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isBuilt() {
        return built;
    }

    public SegmentedBuffer build() {
        segmentation = new Segmentation<>(strategy.getSlices());
        built = true;
        return this;
    }

    protected GuessFactorStats makeStats() {
        return new GuessFactorStats(rollingDepth);
    }

    public GuessFactorStats getStats(double[] vals) {
        GuessFactorStats stats = segmentation.get(vals);
        if(stats == null) {
            stats = makeStats();
            segmentation.add(vals, stats);
        }

        return stats;
    }

    public GuessFactorStats getStats(TargetingLog f) {
        return getStats(strategy.getQuery(f));
    }

    public void log(TargetingLog f, int bucket, double weight) {
        getStats(strategy.getQuery(f)).add(bucket, weight);
    }

    public void log(TargetingLog f, int bucket) {
        getStats(strategy.getQuery(f)).add(bucket, 1.0);
    }

    public void logGuessFactor(TargetingLog f, double gf, double weight) {
        getStats(strategy.getQuery(f)).logGuessFactor(gf, weight);
    }

    public void logGuessFactor(TargetingLog f, double gf) {
        getStats(strategy.getQuery(f)).logGuessFactor(gf, 1.0);
    }

    public boolean isEnabled(Object o) {
        if(condition == null)
            return true;
        return condition.test(o);
    }

    public SegmentedBuffer setCondition(Knn.ParametrizedCondition condition) {
        this.condition = condition;
        return this;
    }

    public void mutate(Knn.ConditionMutation mutation) {
        if(condition != null)
            condition.mutate(mutation);
    }

    private void setupLogging() {
        if(!hasSpecificLog) {
            loggingHit = false;
            loggingBreak = false;
            loggingVirtual = false;
        }
        hasSpecificLog = true;
    }

    public SegmentedBuffer logsHit() {
        setupLogging();
        loggingHit = true;
        return this;
    }

    public SegmentedBuffer logsBreak() {
        setupLogging();
        loggingBreak = true;
        return this;
    }

    public SegmentedBuffer logsVirtual() {
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
}
