package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.utils.geo.AngularRange;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class Shadow {
    public final AngularRange range;
    public final VirtualBullet bullet;
    public final double bearing;

    public Shadow(double bearing, AngularRange range, VirtualBullet bullet) {
        this.range = range;
        this.bearing = bearing;
        this.bullet = bullet;
    }

    public boolean isInside(double angle) {
        return range.isAngleNearlyContained(angle);
    }
}
