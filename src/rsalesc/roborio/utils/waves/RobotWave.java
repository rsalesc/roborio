package rsalesc.roborio.utils.waves;

import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class RobotWave extends Wave {

    public RobotWave(MySnapshot snap, Point source, long time, double velocity) {
        super(snap, source, time, velocity);
    }

    @Override
    public boolean isRobotWave() {
        return true;
    }
}
