package rsalesc.roborio;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.energy.EnergyManager;
import rsalesc.roborio.energy.MirrorPowerManager;
import rsalesc.roborio.energy.TCManager;
import rsalesc.roborio.gunning.AutomaticGun;
import rsalesc.roborio.gunning.RoborioGunArray;
import rsalesc.roborio.movement.DCSurfingMovement;
import rsalesc.roborio.movement.Movement;
import rsalesc.roborio.movement.TCMovement;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.Checkpoint;
import rsalesc.roborio.utils.Clock;
import rsalesc.roborio.utils.R;

import java.awt.*;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class Roborio extends BackAsFrontRobot {
    private static final boolean TC = false;
    private static final boolean MC = false;

    private boolean lostScan = true;
    private static double worstTime = 0;
    private static double totalTime = 0;
    private static long timedTicks = 0;
    private static long skipped = 0;
    private static long lastTime = 0;
    private static double lastSpent = 0;
    private static double lastEnergy = -1;
    private Movement movement;
    private AutomaticGun gun;

    private boolean hasEnded = false;
    private MirrorPowerManager powerPredictor;
    private EnergyManager manager;

    public void run() {
        System.out.println(timedTicks + " " + lastTime + " " + skipped + " " + lastSpent + " " + worstTime);
        timedTicks = 0;
        lastTime = 0;
        lastSpent = 0;
        skipped = 0;
        worstTime = 0;
        lastEnergy = -1;

//        Checkpoint.getInstance().dump();

        clearLastRoundData();
        recoverLastRoundData();

        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setupColors();
        setupRadar();

        if(!TC) {
            powerPredictor = new MirrorPowerManager("power_manager");
            manager = powerPredictor;
        } else {
            manager = new TCManager();
        }

        if(!TC) {
            movement = new DCSurfingMovement(this, "dcsurfing");
//            movement = new GotoSurfingMovement(this);
        } else
            movement = new TCMovement(this);

//        gun = new GuessFactorGun(this, 0.7, false);
//        gun = new DCGuessFactorGun(this, false, "dcgf_gun");
//        gun = new HeadOnGun(this, false);
        gun = new RoborioGunArray(this).setManager(manager).build();

        do {
            innerExecute();
        } while(true);
    }

    public void innerExecute() {
        Checkpoint.getInstance().enter("while");

        try {
            Clock clock = new Clock();

            if(!hasEnded && !lostScan && getEnergy() > R.EPSILON) {
                gun.doFiring();
                movement.doShadowing(gun.getVirtualBullets());
                movement.doMovement();
                if(powerPredictor != null && movement.getLastFireEnemy() != null)
                    powerPredictor.log(MyLog.getInstance().atLeastAt(movement.getLastFireEnemy().getTime()),
                            movement.getLastFireEnemy(),
                            0,
                            0,
                            movement.getLastFirePower());

                gun.doGunning();

                if (getRadarTurnRemaining() == 0)
                    setTurnRadarRight(1);
            } else if(!hasEnded && getEnergy() > R.EPSILON && lostScan){
                activateSpinningLock();
            }

            timedTicks++;
            skipped += Math.max(getRealTime() - lastTime - 1, 0);
            lastTime = getRealTime();
            lastSpent = clock.spent();
            totalTime += clock.spent();
            worstTime = Math.max(worstTime, clock.spent());

            lostScan = true;
            execute();
        } catch(Exception ex) {
            handle(ex);
        }
        Checkpoint.getInstance().leave("while");
    }

    private void trackMe() {
        MyLog.getInstance().push(new MyRobot(this));
    }

    @Override
    public void onWin(WinEvent e) {
        hasEnded = true;
        setTurnRight(Double.POSITIVE_INFINITY);
        activateSpinningLock();
    }

    @Override
    public void onDeath(DeathEvent e) {
        hasEnded = true;
    }

    @Override
    public void onStatus(StatusEvent e) {
        Checkpoint.getInstance().enter("status");
        try {
            super.onStatus(e);
            trackMe();
        } catch(Exception ex) {
            handle(ex);
        }
        Checkpoint.getInstance().leave("status");
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
        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("hit_by");
        try {
            movement.onHitByBullet(e);
        } catch(Exception ex) {
            handle(ex);
        }
        Checkpoint.getInstance().leave("hit_by");
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("bullet_hit");
        try {
            movement.onBulletHit(e);
            gun.onBulletHit(e);
        } catch(Exception ex) {
            handle(ex);
        }
        Checkpoint.getInstance().leave("bullet_hit");
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("track_enemy");
        try {
            if(getEnergy() > R.EPSILON)
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
        Checkpoint.getInstance().leave("track_enemy");
    }

    public void trackEnemy(ScannedRobotEvent e) {
        EnemyTracker.getInstance().push(e, this);
    }

    public void doOnScan(ScannedRobotEvent e) {
        if(getEnergy() < R.EPSILON)
            return;

        lostScan = false;
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
        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("paint");
        g.setColor(Color.BLUE); // default painting color
        movement.onPaint(g);
        gun.onPaint(g);
        Checkpoint.getInstance().leave("paint");
    }

    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        Checkpoint.getInstance().enter("round_ended");
        movement.printLog();
        gun.printLog();
        Checkpoint.getInstance().leave("round_ended");

        System.out.println("Timing Info");
        System.out.println("Average time per tick: " + totalTime / timedTicks);
        System.out.println("Worst time: " + worstTime);
    }
}
