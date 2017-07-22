package roborio.utils;

import roborio.myself.MyRobot;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class MyWave extends Wave {
    private MyRobot robot;

    public MyWave(MyRobot robot) {
        super(robot.getPosition(), robot.getTime(), robot.getVelocity());
        setRobot(robot);
    }

    public MyRobot getRobot() {
        return robot;
    }

    public void setRobot(MyRobot robot) {
        this.robot = robot;
    }
}
