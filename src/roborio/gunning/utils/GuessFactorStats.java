package roborio.gunning.utils;

import roborio.utils.R;
import roborio.utils.Range;

import java.util.Arrays;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class GuessFactorStats implements Cloneable {
    public static final int BUCKET_COUNT = 31;
    public static final int BUCKET_MID = (BUCKET_COUNT - 1) / 2;
    private static final double BUCKET_RATIO = 2.0 / BUCKET_COUNT;
    private final double decay;
    private final double reward;

    public double[] buffer;

    public GuessFactorStats(double decay) {
        this(decay, 0.0);
    }

    public GuessFactorStats(double decay, double reward) {
        this.reward = reward;
        this.decay = decay;
        buffer = new double[BUCKET_COUNT];
    }

    public GuessFactorStats(double[] stats, double decay, double reward) {
        if (stats.length != BUCKET_COUNT)
            throw new IllegalArgumentException();
        buffer = stats;
        this.decay = decay;
        this.reward = reward;
    }

    public GuessFactorStats(double[] stats, double decay) {
        this(stats, decay, 0.0);
    }

    public int getBucket(double alpha) {
        int index = (int)(BUCKET_MID + alpha * BUCKET_MID);
        return R.constrain(0, index, BUCKET_COUNT - 1);
    }

    public double getGuessFactor(int index) {
        return R.constrain(-1, (double)(index - BUCKET_MID) / BUCKET_MID, +1);
    }

    public int getBestBucket(Range range) {
        double acc = buffer[0];
        int best = 0;
        for(int i = 1; i < BUCKET_COUNT; i++) {
            if(range.isNearlyContained(getGuessFactor(i)) && buffer[i] > acc) {
                acc = buffer[i];
                best = i;
            }
        }

        if(R.isNear(buffer[BUCKET_MID], acc))
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
        for(int i = 0; i < BUCKET_COUNT; i++) {
            double influence = (1.0 / (sqr((gf - getGuessFactor(i)) / BUCKET_RATIO ) + 1.0));
            double actualDecay = Math.pow(decay, 1.0 / Math.sqrt(weight));
            buffer[i] *= (1.0 - actualDecay);
            buffer[i] += influence * (actualDecay + reward * weight);
        }
    }

    public void logGuessFactor(double gf) {
        logGuessFactor(gf, 1.0);
    }

    public double getValue(double gf) {
        return getValueFromBucket(getBucket(gf));
    }

    public double getValueFromBucket(int index) {
        return buffer[index];
    }

    public double sqr(double x) {
        return x*x;
    }

    @Override
    public Object clone() {
        double[] buf = Arrays.copyOf(buffer, buffer.length);
        GuessFactorStats res = new GuessFactorStats(buf, this.decay, this.reward);
        return res;
    }
}