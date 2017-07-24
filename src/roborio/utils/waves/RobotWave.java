package roborio.utils.waves;

import roborio.myself.MySnapshot;
import roborio.utils.Point;

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
