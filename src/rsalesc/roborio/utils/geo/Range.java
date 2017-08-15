package rsalesc.roborio.utils.geo;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class Range {
    public double min, max;

    public Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public Range() {
        this(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    public boolean isEmpty() {
        return this.min - R.EPSILON > this.max;
    }

    public double getLength() {
        return Math.max(max-min, 0);
    }

    public boolean isNearlyContained(double x) {
        return R.nearOrBetween(min, x, max);
    }

    public void push(double x) {
        min = Math.min(x, min);
        max = Math.max(x, max);
    }

    public double maxAbsolute() {
        return Math.max(Math.abs(min), Math.abs(max));
    }
    public double minAbsolute() {
        return Math.min(Math.abs(min), Math.abs(max));
    }

    public double getCenter() {
        return (min + max) * 0.5;
    }

    public Range intersect(Range rhs) {
        Range res = new Range(Math.max(min, rhs.min), Math.min(max, rhs.max));
        if(res.isEmpty())
            return new Range();
        return res;
    }
}
