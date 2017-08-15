package rsalesc.roborio.gunning.utils;

import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.stats.GuessFactorStats;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class LinearGuessFactorRange extends GuessFactorRange {
    public LinearGuessFactorRange(double min, double max) {
        super(R.constrain(-1, min, +1), R.constrain(-1, max, +1));
    }

    public LinearGuessFactorRange(double min, double mean, double max) {
        super(R.constrain(-1, min, +1),
                R.constrain(-1, mean, +1),
                R.constrain(-1, max, +1));
    }

    @Override
    public double evaluate(double x) {
        if(R.isNear(max-min, 0))
            return R.isNear(mean, x) ? 1.0 : 0.0;

        if (x < mean)
            return Math.max(0.0, x - min) / (mean - min);
        else
            return Math.max(max - x, 0.0) / (max - mean);
    }


    public GuessFactorStats toStats(double roll) {
        GuessFactorStats res = new GuessFactorStats(roll);
        for(int i = 0; i < GuessFactorStats.BUCKET_COUNT; i++) {
            if(res.getGuessFactor(i) < min && res.getGuessFactor(i+1) > max) {
                double diff = res.getGuessFactor(i+1) - res.getGuessFactor(i);
                res.add(i, evaluate(min) * (mean - res.getGuessFactor(i)) / diff);
                res.add(i+1, evaluate(max) * (res.getGuessFactor(i+1) - mean) / diff);
            } else if(res.getGuessFactor(i) < min && i+1 == GuessFactorStats.BUCKET_COUNT) {
                res.add(i, evaluate(min));
            } else if(res.getGuessFactor(i) > max && i == 0) {
                res.add(i, evaluate(max));
            } else {
                res.add(i, evaluate(res.getGuessFactor(i)));
            }
        }

        return res;
    }
}
