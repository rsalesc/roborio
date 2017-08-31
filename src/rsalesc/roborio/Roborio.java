package rsalesc.roborio;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.energy.*;
import rsalesc.roborio.gunning.RaikoGun;
import rsalesc.roborio.gunning.RoborioGunArray;
import rsalesc.roborio.gunning.utils.GunFireEvent;
import rsalesc.roborio.movement.BaseSurfing;
import rsalesc.roborio.movement.Movement;
import rsalesc.roborio.movement.RoborioMovement;
import rsalesc.roborio.movement.TCMovement;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.*;
import rsalesc.roborio.utils.colors.AnimatedRobotColoring;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class Roborio extends BackAsFrontRobot {
    private final boolean TC = false;
    private final boolean MC = false;
    private final boolean SHIELD = true;

    private boolean lostScan = true;
    private static double worstTime = 0;
    private static double totalTime = 0;
    private static long timedTicks = 0;
    private static long skipped = 0;
    private static long lastTime = 0;
    private static double lastSpent = 0;
    private static double lastEnergy = -1;
    private Movement movement;
    private RoborioGunArray gun;

    private boolean hasEnded = false;
    private MirrorPowerManager powerPredictor;
    private EnergyManager manager;

    private static RaikoGun raikoGun;

    private AnimatedRobotColoring coloring;

    private BulletShield shield;

    public void run() {
//        System.out.println(timedTicks + " " + lastTime + " " + skipped + " " + lastSpent + " " + worstTime);
        timedTicks = 0;
        lastTime = 0;
        lastSpent = 0;
        skipped = 0;
        worstTime = 0;
        lastEnergy = -1;

//        Checkpoint.getInstance().dump();

        clearLastRoundData();
        recoverLastRoundData();

        setupColors();
//        coloring = new AnimatedRobotColoring(this, new Gradient(new Gradient.GradientColor[]{
//            new Gradient.GradientColor(Color.PINK, 0.0),
//            new Gradient.GradientColor(Color.CYAN, 1.0)
//        }), 50);

        HeatTracker.getInstance().setCoolingRate(getGunCoolingRate());

        if(!MC) {
            dissociate();
            setupRadar();
        } else {
            if(raikoGun == null)
                raikoGun = new RaikoGun();
            raikoGun.setup(this);
        }

        if(!TC) {
            powerPredictor = new MirrorPowerManager("power_manager");
            manager = powerPredictor;
        } else {
            manager = new TCManager();
        }

        if(!TC) {
            movement = new RoborioMovement(this).setPowerPredictor(powerPredictor).build();
            if(movement instanceof BaseSurfing && SHIELD && !MC) {
                shield = new BulletShield(this);
            }
        } else {
            movement = new TCMovement(this);
        }

        if(!MC) {
            gun = new RoborioGunArray(this);
            gun.setManager(manager).build();
        }

        do {
            if(!MC)
                innerExecute();
            else if(raikoGun != null)
                innerMC2K7();
            else
                throw new IllegalStateException();
        } while(true);
    }

    private void innerMC2K7() {
        movement.doMovement(false);
        if(powerPredictor != null && movement.getLastFireEnemy() != null)
            powerPredictor.log(MyLog.getInstance().atLeastAt(movement.getLastFireEnemy().getTime()),
                    movement.getLastFireEnemy(),
                    0,
                    0,
                    movement.getLastFirePower());

        raikoGun.doGunning();
        execute();
    }

    public boolean shouldShield() {
        return shield != null && shield.shouldShield();
    }

    public void innerExecute() {
        Checkpoint.getInstance().enter("while");

        try {
            Clock clock = new Clock();
            if(coloring != null)
                coloring.tick(getTime());

            if(!hasEnded && !lostScan && getEnergy() > R.EPSILON) {
                gun.setShielding(shouldShieldNextRound());

                if(!shouldShieldNextRound()) {
                    gun.doFiring();
                } else {
                    GunFireEvent event = shield.doFiring();
                    if(event != null)
                        gun.onFire(event);
                }

                if(!shouldShieldNextRound())
                    movement.doShadowing(gun.getVirtualBullets());

                movement.doMovement(shouldShield());

                if(powerPredictor != null && movement.getLastFireEnemy() != null)
                    powerPredictor.log(MyLog.getInstance().atLeastAt(movement.getLastFireEnemy().getTime()),
                            movement.getLastFireEnemy(),
                            0,
                            0,
                            movement.getLastFirePower());

                if(shouldShield()) {
                    shield.doShielding(((BaseSurfing) movement).getWaves(), EnemyTracker.getInstance().getLatest());
                }

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
            HeatTracker.getInstance().tick(getTime());

            if(movement != null)
                movement.onStatus(e);
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
            HeatTracker.getInstance().onHitByBullet(e);

            if(shouldShield())
                shield.onHitByBullet(e);
            else {
                if(movement != null)
                    movement.onHitByBullet(e);
                if(shouldShieldNextRound())
                    shield.onHitByBullet(e);
            }
        } catch(Exception ex) {
            handle(ex);
        }
        Checkpoint.getInstance().leave("hit_by");
    }

    private boolean shouldShieldNextRound() {
        return shield != null && shield.shouldShieldNextRound();
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        if(hasEnded)
            return;

        try {
            if(shouldShield())
                shield.onBulletHitBullet(e);
             else {
                if(movement != null)
                    movement.onBulletHitBullet(e);
            }

            // TODO: gun here
        } catch(Exception ex) {
            handle(ex);
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("bullet_hit");
        try {
            HeatTracker.getInstance().onBulletHit(e);

            if(!shouldShield()) {
                if (movement != null)
                    movement.onBulletHit(e);
                if(shouldShieldNextRound())
                    shield.onBulletHit(e);
            } else{
                shield.onBulletHit(e);
            }

            if (gun != null)
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

        if(!MC) {
            Checkpoint.getInstance().enter("track_enemy");
            try {
                if (getEnergy() > R.EPSILON)
                    trackEnemy(e);

                HeatTracker.getInstance().push(EnemyTracker.getInstance().getLatestState(e));
            } catch (Exception ex) {
                handle(ex);
            }

            try {
                gun.setShielding(shouldShield()).onScan(e);
                movement.onScan(e);
                doOnScan(e);
            } catch (Exception ex) {
                handle(ex);
            }
            Checkpoint.getInstance().leave("track_enemy");
        } else {
            trackEnemy(e);
            raikoGun.onScannedRobot(e);
            if(movement != null)
                movement.onScan(e);
        }
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
        KeyConfig config = KeyConfig.getInstance();
        config.onPaint(g);

        if(hasEnded)
            return;

        Checkpoint.getInstance().enter("paint");
        g.setColor(Color.BLUE); // default painting color
        if(movement != null)
            movement.onPaint(g);
        if(gun != null)
            gun.onPaint(g);
        if(shield != null)
            shield.onPaint(g);
        Checkpoint.getInstance().leave("paint");
    }

    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        Checkpoint.getInstance().enter("round_ended");
        if(movement != null)
            movement.printLog();
        if(gun != null)
            gun.printLog();
        Checkpoint.getInstance().leave("round_ended");

        System.out.println("Timing Info");
        System.out.println("Average time per tick: " + totalTime / timedTicks);
        System.out.println("Worst time: " + worstTime);
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        KeyConfig config = KeyConfig.getInstance();
        config.toggle(e);

        if(movement != null)
            movement.onKeyPressed(e);
        if(gun != null)
            gun.onKeyPressed(e);
        if(shield != null)
            shield.onKeyPressed(e);
    }
}
