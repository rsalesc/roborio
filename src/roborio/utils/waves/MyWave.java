package roborio.utils.waves;

import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.utils.R;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class MyWave extends RobotWave {
    private MyRobot robot;

    public MyWave(MyLog log, double velocity) {
        super(log.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER), log.getLatest().getPoint(), log.getLatest().getTime(), velocity);
        setRobot(robot);
    }

    public MyRobot getRobot() {
        return robot;
    }

    public void setRobot(MyRobot robot) {
        this.robot = robot;
    }
}
