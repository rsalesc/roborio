package rsalesc.roborio.movement;

import robocode.*;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.energy.MirrorPowerManager;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.utils.BackAsFrontRobot;

import java.awt.*;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class Movement {
    private BackAsFrontRobot robot;
    private ComplexEnemyRobot lastFireEnemy;
    private double lastFirePower;
    private MirrorPowerManager powerPredictor;

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

    public void onStatus(StatusEvent e) {};

    public void onBulletHit(BulletHitEvent e) {}
    public void onHitByBullet(HitByBulletEvent e) {}
    public void onBulletHitBullet(BulletHitBulletEvent e) {}

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

    public void moveWithBackAsFront(rsalesc.roborio.utils.geo.Point dest, double distance) {
        robot.moveWithBackAsFront(dest, distance);
    }

    public void moveWithBackAsFront(rsalesc.roborio.utils.geo.Point dest) {
        robot.moveWithBackAsFront(dest);
    }

    public void runAwayWithBackAsFront(rsalesc.roborio.utils.geo.Point dest, double distance) {
        robot.runAwayWithBackAsFront(dest, distance);
    }

    public void printLog() {}

    public void onFire(ComplexEnemyRobot enemy, double power) {
        lastFireEnemy = enemy;
        lastFirePower = power;
    }

    public ComplexEnemyRobot getLastFireEnemy() {
        return lastFireEnemy;
    }

    public double getLastFirePower() {
        return lastFirePower;
    }

    public void clearLastFire() {
        lastFireEnemy = null;
        lastFirePower = 0;
    }

    public Movement setPowerPredictor(MirrorPowerManager m) {
        this.powerPredictor = m;
        return this;
    }

    // TODO: pensar melhor no build
    public Movement build() {
        return this;
    }

    public MirrorPowerManager getPowerPredictor() {
        return powerPredictor;
    }
}
