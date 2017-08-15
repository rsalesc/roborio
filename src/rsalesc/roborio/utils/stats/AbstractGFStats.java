package rsalesc.roborio.utils.stats;

import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Range;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public abstract class AbstractGFStats extends VCS implements Cloneable {
    public static final int BUCKET_COUNT = 101;
    public static final int BUCKET_MID = (BUCKET_COUNT - 1) / 2;
    private static final double BUCKET_RATIO = 2.0 / BUCKET_COUNT;
    private final double reward;

    public AbstractGFStats(double rollingDepth) {
        this(rollingDepth, 0.0);
    }

    public AbstractGFStats(double rollingDepth, double reward) {
        super(BUCKET_COUNT, rollingDepth);
        this.reward = reward;
    }

    public AbstractGFStats(double[] stats, double rollingDepth, double reward) {
        super(BUCKET_COUNT, rollingDepth);
        if (stats.length != BUCKET_COUNT)
            throw new IllegalArgumentException();
        buffer = stats;
        this.reward = reward;
    }

    public AbstractGFStats(double[] stats, double rollingDepth) {
        this(stats, rollingDepth, 0.0);
    }

    public int getBucket(double alpha) {
        int index = (int)(BUCKET_MID + alpha * BUCKET_MID);
        return R.constrain(0, index, BUCKET_COUNT - 1);
    }

    public double getGuessFactor(int index) {
        return R.constrain(-1, (double)(index - BUCKET_MID) / BUCKET_MID, +1);
    }

    public int getBestBucket(Range range) {
        double acc = get(0);
        int best = 0;
        for(int i = 1; i < BUCKET_COUNT; i++) {
            if(range.isNearlyContained(getGuessFactor(i)) && get(i) > acc) {
                acc = get(i);
                best = i;
            }
        }

        if(R.isNear(get(BUCKET_MID), acc))
            return BUCKET_MID;

        return best;
    }

    public int getBestBucket() {
        return getBestBucket(new Range(-1.0, 1.0));
    }

    public double getBestGuessFactor() {
        return getGuessFactor(getBestBucket());
    }

    public double getBestGuessFactor(Range range) {
        return getGuessFactor(getBestBucket(range));
    }

    public void logGuessFactor(double gf, double weight) {
        add(getBucket(gf), weight);
    }

    public void logGuessFactor(double gf) {
        logGuessFactor(gf, 1.0);
    }

    public double getValue(double gf) {
        return getValueFromBucket(getBucket(gf));
    }

    public double getValueFromBucket(int index) {
        return get(index);
    }

    public double sqr(double x) {
        return x*x;
    }
}