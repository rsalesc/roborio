package roborio.utils.stats;

import roborio.utils.stats.smoothing.HarmonicSmoothing;

import java.util.Arrays;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class GuessFactorStats extends AbstractGFStats implements Cloneable {
    public GuessFactorStats(double rollingDepth) {
        super(rollingDepth);
        setDefaultSmoother();
    }

    public GuessFactorStats(double rollingDepth, double reward) {
        super(rollingDepth, reward);
        setDefaultSmoother();
    }

    public GuessFactorStats(double[] stats, double rollingDepth, double reward) {
        super(stats, rollingDepth, reward);
        setDefaultSmoother();
    }

    public GuessFactorStats(double[] stats, double rollingDepth) {
        super(stats, rollingDepth);
        setDefaultSmoother();
    }

    private void setDefaultSmoother() {
        setSmoother(new HarmonicSmoothing(Math.sqrt(4.0)));
    }

    @Override
    public Object clone() {
        double[] newBuffer = Arrays.copyOf(buffer, buffer.length);
        GuessFactorStats res = new GuessFactorStats(newBuffer, getRollingDepth(), 1.0);
        return res;
    }

    public static GuessFactorStats merge(GuessFactorStats[] sts, double[] weights) {
        int size = BUCKET_COUNT;
        double[] buffer = new double[size];
        for(int i = 0; i < sts.length; i++) {
            GuessFactorStats normalized = (GuessFactorStats) (sts[i].clone());
            normalized.normalize();
            for(int j = 0; j < size; j++) {
                buffer[j] += normalized.getFlat(j) * weights[i];
            }
        }

        return new GuessFactorStats(buffer, Double.POSITIVE_INFINITY);
    }
}
