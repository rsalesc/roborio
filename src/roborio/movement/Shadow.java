package roborio.movement;

import robocode.util.Utils;
import roborio.utils.geo.Range;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class Shadow {
    public Range range;
    public double bearing;

    public Shadow(double bearing, Range range) {
        this.range = range;
        this.bearing = bearing;
    }

    public boolean isInside(double angle) {
        return range.isNearlyContained(Utils.normalRelativeAngle(angle - bearing));
    }
}
