package roborio;

import robocode.HitByBulletEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.EnemyTracker;
import roborio.gunning.AutomaticGun;
import roborio.gunning.GuessClusteringGun;
import roborio.movement.RoborioMovement;
import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.utils.BackAsFrontRobot;

import java.awt.*;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class Roborio extends BackAsFrontRobot {

    private double worstTime = 0;
    private double totalTime = 0;
    private int timedTicks = 0;
    private RoborioMovement movement;
    private AutomaticGun gun;

    public void run() {
        clearLastRoundData();
        recoverLastRoundData();

        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setupColors();
        setupRadar();
        movement = new RoborioMovement(this);
        gun = new GuessClusteringGun(this, false);

        while(true) {
            double startTime = System.nanoTime();

            trackMe();
            gun.doGunning();
            movement.doMovement();

            if(getRadarTurnRemaining() == 0)
                setTurnRadarRight(1);

            gun.doFiring();

            double timeTaken = System.nanoTime() - startTime;
            timedTicks++;
            worstTime = Math.max(worstTime, timeTaken);
            totalTime += timeTaken;

            execute();
        }
    }

    private void trackMe() {
        MyLog.getInstance().push(new MyRobot(this));
    }

    void activateSpinningLock() {
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    void setupRadar() {
        activateSpinningLock();
    }

    void setupColors() {
        setColors(Color.BLACK, Color.YELLOW, Color.BLACK);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        movement.onHitByBullet(e);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        trackEnemy(e);

        gun.onScan(e);
        movement.onScan(e);
        doOnScan(e);
    }

    public void trackEnemy(ScannedRobotEvent e) {
        EnemyTracker.getInstance().push(e, this);
    }

    public void doOnScan(ScannedRobotEvent e) {
        double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn)*2);
    }

    public boolean isOn1v1() {
        return getOthers() <= 1;
    }
    public boolean isOnMelee() {
        return !isOn1v1();
    }

    void clearLastRoundData() {
        EnemyTracker.getInstance().clear();
    }

    void recoverLastRoundData() {

    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(Color.BLUE); // default painting color
        movement.onPaint(g);
        gun.onPaint(g);
    }

    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        movement.printLog();
        gun.printLog();

        System.out.println("Timing Info");
        System.out.println("Average time per tick: " + totalTime / timedTicks / 1000000);
        System.out.println("Worst time: " + worstTime / 1000000);
    }
}
