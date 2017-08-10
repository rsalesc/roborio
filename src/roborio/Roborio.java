package roborio;

import robocode.*;
import robocode.util.Utils;
import roborio.enemies.EnemyTracker;
import roborio.gunning.AutomaticGun;
import roborio.gunning.DCGuessFactorGun;
import roborio.movement.Movement;
import roborio.movement.TCMovement;
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
    private Movement movement;
    private AutomaticGun gun;

    public void run() {
        clearLastRoundData();
        recoverLastRoundData();

        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setupColors();
        setupRadar();

        movement = new TCMovement(this);
//        movement = new DCSurfingMovement(this, "dcsurfing");
//        movement = new GotoSurfingMovement(this);

//        gun = new DualGuessFactorGun(this);
        gun = new DCGuessFactorGun(this, false, "dcgf_gun");
//        gun = new HeadOnGun(this, false);

        while(true) {
            double startTime = System.nanoTime();

            gun.doFiring();
            gun.doGunning();
            movement.doShadowing(gun.getVirtualBullets());
            movement.doMovement();

            if(getRadarTurnRemaining() == 0)
                setTurnRadarRight(1);

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

    @Override
    public void onStatus(StatusEvent e) {
        try {
            super.onStatus(e);
            trackMe();
        } catch(Exception ex) {
            handle(ex);
        }
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

        try {
            movement.onHitByBullet(e);
        } catch(Exception ex) {
            handle(ex);
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        try {
            movement.onBulletHit(e);
            gun.onBulletHit(e);
        } catch(Exception ex) {
            handle(ex);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        try {
            trackEnemy(e);
        } catch(Exception ex) {
            handle(ex);
        }

        try {
            gun.onScan(e);
            movement.onScan(e);
            doOnScan(e);
        } catch(Exception ex) {
            handle(ex);
        }
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

    private void clearLastRoundData() {
        EnemyTracker.getInstance().clear();
    }

    private void recoverLastRoundData() {

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

    @Override
    public void onSkippedTurn(SkippedTurnEvent e) {
        System.out.println("Skipped turn " + e.getSkippedTurn());
    }
}
