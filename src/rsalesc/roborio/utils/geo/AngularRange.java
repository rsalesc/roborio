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

    public AngularRange translate(double newReference) {
        double delta = newReference - reference;
        return new AngularRange(newReference, min - delta, max - delta);
    }

    // TODO: handle cases where translation is absurd
    public AngularRange intersectAngles(AngularRange rhs) {
        AngularRange translated = rhs.translate(reference);
        Range res = intersect(translated);
        return new AngularRange(reference, res.min, res.max);
    }
}
