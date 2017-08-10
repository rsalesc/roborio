package roborio.movement;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import roborio.gunning.utils.VirtualBullet;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.geo.Point;

import java.awt.*;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class Movement {
    private BackAsFrontRobot robot;

    public Movement(BackAsFrontRobot robot) {
        setRobot(robot);
    }

    public BackAsFrontRobot getRobot() {
        return robot;
    }

    public void setRobot(BackAsFrontRobot robot) {
        this.robot = robot;
    }

    public abstract void doMovement();
    public void doShadowing(VirtualBullet[] bullets) {}
    public void onScan(ScannedRobotEvent e) {}

    public void onBulletHit(BulletHitEvent e) {}

    public void onHitByBullet(HitByBulletEvent e) {}
    public void onPaint(Graphics2D g) {}

    public long getTime()  { return robot.getTime(); }

    public void setTurnRight(double degrees) {
        robot.setTurnRight(degrees);
    }

    public void setTurnRightRadians(double degrees) {
        robot.setTurnRightRadians(degrees);
    }

    public double getTurnRemaining() {
        return robot.getTurnRemaining();
    }

    public double getTurnRemainingRadians() {
        return robot.getTurnRemainingRadians();
    }

    public void setBackAsFront(double angle) {
        robot.setBackAsFront(angle);
    }

    public void setBackAsFront(double angle, double distance) {
        robot.setBackAsFront(angle, distance);
    }

    public void moveWithBackAsFront(Point dest, double distance) {
        robot.moveWithBackAsFront(dest, distance);
    }

    public void moveWithBackAsFront(Point dest) {
        robot.moveWithBackAsFront(dest);
    }

    public void runAwayWithBackAsFront(Point dest, double distance) {
        robot.runAwayWithBackAsFront(dest, distance);
    }

    public void printLog() {}
}
