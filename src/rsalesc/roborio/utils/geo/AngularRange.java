package rsalesc.roborio.utils.geo;

import robocode.util.Utils;

/**
 * Created by Roberto Sales on 16/08/17.
 */
public class AngularRange extends Range {
    private double reference;

    public AngularRange(double reference, double min, double max) {
        super(min, max);
        this.reference = reference;
    }

    public AngularRange(double reference, Range range) {
        super(range.min, range.max);
        this.reference = reference;
    }

    public double getAngle(double offset) {
        return Utils.normalAbsoluteAngle(reference + offset);
    }

    public double getOffset(double angle) {
        return Utils.normalRelativeAngle(angle - reference);
    }

    public double getStartingAngle() {
        return getAngle(min);
    }

    public double getEndingAngle() {
        return getAngle(max);
    }

    public void pushAngle(double angle) {
        push(getOffset(angle));
    }

    public boolean isAngleNearlyContained(double angle) {
        return isNearlyContained(getOffset(angle));
    }
}
