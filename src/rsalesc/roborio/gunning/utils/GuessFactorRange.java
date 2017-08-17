package rsalesc.roborio.gunning.utils;

import rsalesc.roborio.utils.geo.Range;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class GuessFactorRange extends Range {
    public double mean;

    public GuessFactorRange(double min, double max) {
        super(min, max);
        this.mean = (min + max) / 2;
    }

    public GuessFactorRange(double min, double mean, double max) {
        super(min, max);
        this.mean = mean;
    }
}
