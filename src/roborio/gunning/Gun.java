package roborio.gunning;

import robocode.Bullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.utils.BackAsFrontRobot;

import java.awt.*;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public abstract class Gun {
    private BackAsFrontRobot robot;

    public Gun(BackAsFrontRobot robot) {
        setRobot(robot);
    }

    public abstract void doGunning();
    public void onScan(ScannedRobotEvent e) {}
    public void onPaint(Graphics2D g) {}

    public BackAsFrontRobot getRobot() {
        return robot;
    }

    public void setRobot(BackAsFrontRobot robot) {
        this.robot = robot;
    }

    public double getGunTurnRemainingRadians() {
        return robot.getGunTurnRemainingRadians();
    }

    public double getGunHeat() {
        return robot.getGunHeat();
    }

    public double getGunHeadingRadians() {
        return robot.getGunHeadingRadians();
    }

    public void setTurnGunRightRadians(double radians) {
        robot.setTurnGunRightRadians(radians);
    }

    public void setTurnGunToRadians(double radians) {
        setTurnGunRightRadians(Utils.normalRelativeAngle(radians - getGunHeadingRadians()));
    }

    public void setFire(double power) {
        robot.setFire(power);
    }

    public Bullet setFireBullet(double power) {
        return robot.setFireBullet(power);
    }

    public void printLog(){}
}
