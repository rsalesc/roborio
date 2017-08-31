package rsalesc.roborio.energy;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.gunning.utils.GunFireEvent;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.movement.ShadowManager;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.KeyConfig;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.Circle;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Roberto Sales on 29/08/17.
 */
public class BulletShield {
    private static KeyConfig config = KeyConfig.getInstance();

    private static final double[]  MOVE_WEIGHTS = {0.2, 1.0, 0.5};

    private static final double ERROR = 0.00001;
    private static final double MOVE_AMOUNT = 0.4;

    private static final Candidate[] candidates = new Candidate[]{
            new Candidate() {
                @Override
                public double getAngle(EnemyLog log, MyLog myLog, long time) {
                    return Physics.absoluteBearing(log.atLeastAt(time - 1).getPoint(),
                            myLog.atLeastAt(time - 1).getPoint());
                }
            },
            new Candidate() {
                @Override
                public double getAngle(EnemyLog log, MyLog myLog, long time) {
                    return Utils.normalAbsoluteAngle(Physics.absoluteBearing(log.atLeastAt(time - 1).getPoint(),
                            myLog.atLeastAt(time - 1).getPoint())
                            + log.atLeastAt(time).getHeading()
                            - log.atLeastAt(time - 1).getHeading());
                }
            },
            new Candidate() {
                @Override
                public double getAngle(EnemyLog log, MyLog myLog, long time) {
                    return Physics.absoluteBearing(log.atLeastAt(time).getPoint(),
                            myLog.atLeastAt(time - 1).getPoint());
                }
            },
            new Candidate() {
                @Override
                public double getAngle(EnemyLog log, MyLog myLog, long time) {
                    ComplexEnemyRobot enemy = log.atLeastAt(time - 1);
                    Point next = enemy.getPoint().project(enemy.getHeading(), enemy.getVelocity());
                    return Physics.absoluteBearing(next,
                            myLog.atLeastAt(time - 1).getPoint());
                }
            }
    };

    private static final double MINIMUM_SCORE = 0.96;
    private static int fired = 0;
    private static int damageTaken = 0;
    private static int damageInflicted = 0;
    private static boolean stoppedShielding = false;

    private boolean stoppedForThisRound = false;

    private BackAsFrontRobot robot;
    private Long lastMove;
    private Double movedAmount;
    private ShieldingOption nextOption;

    private WaveCollection myWaves = new WaveCollection();
    private WaveCollection lastWaves;
    private HashMap<Wave, double[][]> tried;

    private HashSet<ShieldingOption> firedOptions = new HashSet<>();
    private HashSet<Wave>            hasHit = new HashSet<>();

    private ComplexEnemyRobot lastEnemy;

    public BulletShield(BackAsFrontRobot robot) {
        this.robot = robot;
        tried = new HashMap<>();
    }

    private boolean _shouldShield() {
        if(robot.getTime() > 90 && lastEnemy == null)
            return false;
        else if(lastEnemy == null)
            return true;

        boolean timeMargin = (robot.getRoundNum() == 0 && robot.getTime() > 50) || robot.getRoundNum() > 0;
        boolean biggerTimeMargin = robot.getRoundNum() > 0;


        EnemyLog log = EnemyTracker.getInstance().getLog(lastEnemy);

        if(timeMargin && log.isRamming() && log.getLatest().getEnergy() > 0.1)
            return false;

        if(damageTaken > getMaximumEnemyScore())
            return false;

        if(biggerTimeMargin && (double) getBestScore() < fired * 0.63)
            return false;

        return true;
    }

    public boolean shouldShield() {
        boolean stopNow = false;

        if(lastEnemy != null) {
            EnemyLog log = EnemyTracker.getInstance().getLog(lastEnemy);

            stopNow = log.isRamming() && log.getLatest().getEnergy() < 0.1 ||
                    (log.getLatest().getDistance() < 90 && robot.getTicksToCool() > 4 && robot.getTime() > 50);
        }

        stoppedShielding = stoppedShielding || !_shouldShield();
        stoppedForThisRound = stoppedShielding || stoppedForThisRound || stopNow;
        return !stoppedForThisRound;
    }

    public boolean shouldShieldNextRound() {
        shouldShield();
        return !stoppedShielding;
    }

    private long getBestScore() {
        long res = 0;
        for(Candidate candidate : candidates)
            for(int j = 0; j < 3; j++)
                if(candidate.score[j] > res)
                    res = candidate.score[j];
        return res;
    }

    public double getMaximumEnemyScore() {
        return (damageInflicted + 60.0 * robot.getNumRounds()) * (1.0 - MINIMUM_SCORE) / MINIMUM_SCORE;
    }

    public boolean isSuffering() {
        return robot.getIdleTicks() > 100 && lastEnemy.getEnergy() > robot.getEnergy() - 1.5;
    }

    public boolean canFire() {
        return robot.getGunHeat() == 0 && robot.getGunTurnRemaining() == 0 && robot.getDistanceRemaining() == 0;
    }

    public GunFireEvent doFiring() {
        if(canFire() && nextOption != null) {
            Bullet bullet = robot.setFireBullet(nextOption.power);
            firedOptions.add(nextOption);
            myWaves.add(new MyBulletWave(MyLog.getInstance(), bullet));
            fired++;

            return new GunFireEvent(null, robot.getPoint(), bullet, robot.getTime());
//            return null;
        }

        return null;
    }

    public void tickWaves() {
        Iterator<Wave> wit = myWaves.iterator();

        while(wit.hasNext()) {
            Wave w = wit.next();
            if(w.getDistanceTraveled(robot.getTime()) > 800)
                wit.remove();
        }

        Iterator<Map.Entry<Wave, double[][]>> iterator = tried.entrySet().iterator();

        while(iterator.hasNext()) {
            Wave wave = iterator.next().getKey();
            if(wave.getDistanceTraveled(robot.getTime()) > 1000)
                iterator.remove();
        }

        Iterator<ShieldingOption> its = firedOptions.iterator();

        while(its.hasNext()) {
            ShieldingOption option = its.next();
            if(robot.getTime() - 12 > option.breakTime)
                its.remove();
        }

        wit = hasHit.iterator();

        while(wit.hasNext()) {
            Wave w = wit.next();
            if(w.getDistanceTraveled(robot.getTime()) > 800)
                wit.remove();
        }
    }

    public void doShielding(WaveCollection waves, ComplexEnemyRobot anyEnemy) {
        lastWaves = waves;
        lastEnemy = anyEnemy;

        if(isSuffering() && lastEnemy.getEnergy() + 0.15 > robot.getEnergy())
            damageTaken++;

        tickWaves();

        // undo movement
        if(lastMove != null && lastMove + 1 <= robot.getTime()) {
            robot.setTurnRightRadians(0);
            robot.setAhead(-movedAmount);
            lastMove = null;
            movedAmount = null;
        } else if(lastMove != null) {
            robot.setTurnRightRadians(0);
        } else {
            robot.setTurnTo(Utils.normalAbsoluteAngle(anyEnemy.getAbsoluteBearing() + R.HALF_PI));
        }

        boolean doShit = robot.getTicksToCool() <= 1;

        double bestTime = Double.POSITIVE_INFINITY;
        EnemyFireWave bestWave = null;

        Point curPosition = robot.getPoint();

        for(Wave wave : waves) {
            // shield against imaginary waves?
            if(wave instanceof EnemyFireWave && !wave.hasTouchedRobot(curPosition, robot.getTime() + 1) // maybe plus 2?
                && !((EnemyFireWave) wave).isImaginary() && !tried.containsKey(wave) && !hasHit.contains(wave)) {
                EnemyFireWave w = (EnemyFireWave) wave;

                if(w.getBreakTime(curPosition) < bestTime) {
                    bestTime = w.getBreakTime(curPosition);
                    bestWave = w;
                }
            }
        }

        boolean isStationaryFine;
        ShieldingOption option = null;

        if(bestWave != null) {
            double impactDelta = bestTime - (robot.getTime() + 1);

            double fireAngle = getBestAngle(EnemyTracker.getInstance().getLog(bestWave.getEnemy()),
                    MyLog.getInstance(), robot.getTime());

            boolean predictorWin = isBestPredictor(EnemyTracker.getInstance().getLog(bestWave.getEnemy()),
                    MyLog.getInstance(), robot.getTime());

            isStationaryFine =
                    Math.abs(bestWave.getEnemy().getVelocity() * Math.sin(bestWave.getEnemy().getHeading() - fireAngle)) > 0.11
                            && predictorWin;

            {
                double posAhead = fired % 2 == 0 ? MOVE_AMOUNT : -MOVE_AMOUNT;
                double negAhead = fired % 2 == 1 ? MOVE_AMOUNT : -MOVE_AMOUNT;

                double rollback = robot.getDistanceRemaining();

                ShieldingOption stopOption = getShieldingOption(bestWave, MovingOption.STOP);
                robot.setAhead(posAhead);
                ShieldingOption posOption = getShieldingOption(bestWave,
                        fired % 2 == 0 ? MovingOption.FORWARD : MovingOption.BACKWARD);
                robot.setAhead(negAhead);
                ShieldingOption negOption = getShieldingOption(bestWave,
                        fired % 2 == 0 ? MovingOption.BACKWARD : MovingOption.FORWARD);
                robot.setAhead(rollback);

                if(isStationaryFine) {
                    option = stopOption;
                } else {
                    if(stopOption.width > posOption.width && stopOption.width > negOption.width) {
                        option = stopOption;
                    } else if(posOption.width > negOption.width) {
                        option = posOption;
                        robot.setAhead(posAhead);
                        movedAmount = posAhead;
                        lastMove = robot.getTime();
                    } else {
                        option = negOption;
                        robot.setAhead(negAhead);
                        movedAmount = negAhead;
                        lastMove = robot.getTime();
                    }
                }
            }
        }

        if(option != null) {
            if(doShit) {
                tried.put(bestWave, getEveryAngle(EnemyTracker.getInstance().getLog(bestWave.getEnemy()),
                        MyLog.getInstance(), bestWave.getTime()));
            }

            robot.setGunTo(option.angle);
            nextOption = option;
        } else {
            nextOption = null;
            double absBearing = Physics.absoluteBearing(robot.getNextPosition(), anyEnemy.getPoint());
            robot.setGunTo(absBearing);
        }
    }

    public boolean safeMove(ComplexEnemyRobot enemy) {
        HeatLog heatLog = HeatTracker.getInstance().ensure(enemy);
        long ticksToFire = heatLog.ticksToCool();

        // it isnt safe to move if the enemy will shoot in less than three ticks
        // thats because it may use past info and adjust the gun in the wrong way as well
        boolean danger = ticksToFire < 4;

        // try not to move when bulleting hit events are occurring
        for(ShieldingOption option : firedOptions) {
            if(R.isBetween(option.breakTime - 3, robot.getTime(), option.breakTime + 2))
                danger = true;
        }

        // try not to move when being hit
        for(Wave wave : lastWaves) {
            if(hasHit.contains(wave))
                continue;

            long breakTime = (int) Math.ceil(wave.getBreakTime(robot.getPoint()));
            if(R.isBetween(breakTime - 3, robot.getTime(), breakTime + 4))
                danger = true;
        }

        return !danger;
    }

    public ShieldingOption getShieldingOption(EnemyFireWave w, MovingOption moveOption) {
        if(w == null)
            return null;

        ComplexEnemyRobot enemy = w.getEnemy();
        EnemyLog log = EnemyTracker.getInstance().getLog(enemy);
        ComplexEnemyRobot latestEnemy = log.getLatest();

        long fireTime = robot.getTime() + 1;

        // traditional head-on angle
        double fireAngle = getBestAngle(log, MyLog.getInstance(), w.getTime());

        double accWidth = -1;
        double accPower = 0.1;
        double accAngle = 0;
        AngularRange accRange = null;
        long accBreakTime = 0;

        Point myPosition = robot.getNextPosition();
        double newBearing = Physics.absoluteBearing(myPosition, enemy.getPoint());

        double absDelta = Math.abs(Utils.normalRelativeAngle(newBearing + R.PI - fireAngle));

        // CREDITS: jk / EnergyDome
        double maxBP = Math.min(w.getPower() - 0.01, w.getPower()*(robot.getEnergy()-10)/latestEnemy.getEnergy());

        double ratio = Math.max(maxBP / 25, 0.01);

        for (double bp = 0.1; bp < Math.max(maxBP + .0001, 0.101); bp += ratio) {
            double speed = Rules.getBulletSpeed(bp);

            AngularRange range =
                    ShadowManager.getShadow(new Wave(MyLog.getInstance().takeSnapshot(2), myPosition, fireTime, speed),
                            new VirtualBullet(w.getSource(), fireAngle, w.getPower(), w.getTime()));

            if (range == null)
                continue;

            double width = range.getLength();
            double myAngle = range.getAngle(range.getCenter());

            long breakTime = getBreakTime(w, fireAngle, myPosition, fireTime, speed);

            if (width > accWidth) {
                accWidth = width;
                accPower = bp;
                accAngle = myAngle;
                accRange = range;
                accBreakTime = breakTime;
            }
        }

        if(accRange == null)
            return new ShieldingOption(0, 0, 0, 0);

        return new ShieldingOption(accAngle, accPower, accWidth * MOVE_WEIGHTS[moveOption.ordinal()] * absDelta, accBreakTime);
    }

    public double getBestAngle(EnemyLog log, MyLog myLog, long time) {
        long best = -1;
        double bestAngle = 0;

        for(int j = 0; j < 3; j++) {
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i].score[j] > best) {
                    bestAngle = candidates[i].getAngle(log, myLog, time, j);
                    best = candidates[i].score[j];
                }
            }
        }

        return bestAngle;
    }

    public double[][] getEveryAngle(EnemyLog log, MyLog myLog, long time) {
        double[][] res = new double[candidates.length][3];

        for(int i = 0; i < candidates.length; i++) {
            for(int j = 0; j < 3; j++) {
                res[i][j] = candidates[i].getAngle(log, myLog, time, j);
                res[i][j] = candidates[i].getAngle(log, myLog, time, j);
                res[i][j] = candidates[i].getAngle(log, myLog, time, j);
            }
        }

        return res;
    }

    public boolean isBestStationary(EnemyLog log, MyLog myLog, long time) {
        boolean stationary = false;
        long best = -1;

        for(int j = 0; j < 3; j++) {
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i].score[j] > best) {
                    stationary = j < 2;
                    best = candidates[i].score[j];
                }
            }
        }

        return stationary;
    }

    public boolean isBestPredictor(EnemyLog log, MyLog myLog, long time) {
        boolean predictor = false;
        long best = -1;

        for(int j = 0; j < 3; j++) {
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i].score[j] > best) {
                    predictor = i < 2;
                    best = candidates[i].score[j];
                }
            }
        }

        return predictor;
    }


    public long getBreakTime(Wave wave, double fireAngle, Point mySource, long time, double mySpeed) {
        long left = time, right = time + 150;
        for(long mid = left; mid < right; mid++) {
            Circle myCircle = new Circle(mySource, mySpeed * (mid - time));
            if(myCircle.isInside(wave.project(fireAngle, mid))) {
                left = mid;
                break;
            }
        }

        if(left == time + 150 || wave.getDistanceTraveled(left) >= mySource.distance(wave.getSource()))
            return -1;

        return left;
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        EnemyFireWave w = (EnemyFireWave) R.getFirer(lastWaves, e.getHitBullet(), robot.getTime());

        if(w == null) {
            System.out.println("missed a bullet collision on shielding");
            return;
        }

        ShieldingOption option = null;
        for(ShieldingOption opt : firedOptions) {
            if(R.isBetween(opt.breakTime - 1, robot.getTime(), opt.breakTime + 1))
                option = opt;
        }

        firedOptions.remove(option);

        ComplexEnemyRobot enemy = w.getEnemy();
        EnemyLog log = EnemyTracker.getInstance().getLog(enemy);
        ComplexEnemyRobot pastEnemy = EnemyTracker.getInstance().getLog(enemy).atLeastAt(w.getTime() - 1);

        double expectedAngle = getBestAngle(log, MyLog.getInstance(), w.getTime());
        double actualAngle = e.getHitBullet().getHeadingRadians();

//        System.out.println("bullet hit bullet: " + Utils.normalRelativeAngle(actualAngle - expectedAngle));

        if(tried.containsKey(w)) {
            double[][] expected = tried.get(w);

            for(int i = 0; i < candidates.length; i++) {
                candidates[i].score(actualAngle, expected[i], Candidate.getDirection(log, MyLog.getInstance(), w.getTime()));
            }
            tried.remove(w);
        }

        hasHit.add(w);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        EnemyFireWave w = (EnemyFireWave) R.getFirer(lastWaves, e.getBullet(), robot.getTime());

        damageTaken += Rules.getBulletDamage(e.getPower());

        if(w == null) {
            System.out.println("missed a bullet hit on shielding");
            return;
        }

        ComplexEnemyRobot enemy = w.getEnemy();
        EnemyLog log = EnemyTracker.getInstance().getLog(enemy);
        ComplexEnemyRobot pastEnemy = EnemyTracker.getInstance().getLog(enemy).atLeastAt(w.getTime() - 1);

        double expectedAngle = getBestAngle(log, MyLog.getInstance(), w.getTime());
        double actualAngle = e.getBullet().getHeadingRadians();

//        System.out.println("bullet hit me: " + Utils.normalRelativeAngle(actualAngle - expectedAngle));

        if(tried.containsKey(w)) {
            double[][] expected = tried.get(w);

            for(int i = 0; i < candidates.length; i++) {
                candidates[i].score(actualAngle, expected[i], Candidate.getDirection(log, MyLog.getInstance(), w.getTime()));
            }
            tried.remove(w);
        }

        hasHit.add(w);
    }

    public void onBulletHit(BulletHitEvent e) {
        damageInflicted += Rules.getBulletDamage(e.getBullet().getPower());
    }

    private abstract static class Candidate {
        private long trainings = 0;
        private double offset = 0;
        private double directedOffset = 0;

        private long[] score;

        public Candidate() {
            score = new long[3];
        }

        public abstract double getAngle(EnemyLog log, MyLog myLog, long time);

        public double getDisplacedAngle(EnemyLog log, MyLog myLog, long time) {
            return Utils.normalAbsoluteAngle(getAngle(log, myLog, time) + offset);
        }

        public double getDirectedDisplaceAngled(EnemyLog log, MyLog myLog, long time) {
            int direction = getDirection(log, myLog, time);
            return Utils.normalAbsoluteAngle(getAngle(log, myLog, time) + directedOffset * direction);
        }

        public double getAngle(EnemyLog log, MyLog myLog, long time, int index) {
            if(index == 0)
                return getAngle(log, myLog, time);
            else if(index == 1)
                return getDisplacedAngle(log, myLog, time);
            else
                return getDirectedDisplaceAngled(log, myLog, time);
        }

        public static int getDirection(EnemyLog log, MyLog myLog, long time) {
            int res = myLog.atLeastAt(time - 1)
                    .getDirection(log.atLeastAt(time - 1).getPoint());
            if(res == 0)
                return 1;
            return res;
        }

        public void score(double actualAngle, EnemyLog log, MyLog myLog, long time) {
            double angle = getAngle(log, myLog, time);
            double displacedAngle = getDisplacedAngle(log, myLog, time);
            double directedDisplacedAngle = getDirectedDisplaceAngled(log, myLog, time);
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - angle), 0, ERROR))
                score[0]++;
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - displacedAngle), 0, ERROR))
                score[1]++;
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - directedDisplacedAngle), 0, ERROR))
                score[2]++;

            train(Utils.normalRelativeAngle(actualAngle - angle), getDirection(log, myLog, time));
        }

        public void score(double actualAngle, double[] expectedAngles, int direction) {
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - expectedAngles[0]), 0, ERROR))
                score[0]++;
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - expectedAngles[1]), 0, ERROR))
                score[1]++;
            if(R.isNear(Utils.normalRelativeAngle(actualAngle - expectedAngles[2]), 0, ERROR))
                score[2]++;

//            dumpScores();

            train(Utils.normalRelativeAngle(actualAngle - expectedAngles[0]), direction);
        }

        private void train(double delta, int direction) {
            offset = (offset * trainings + delta) / (trainings + 1);  // roll
            directedOffset = (directedOffset * trainings + delta * direction) / (trainings + 1);
            trainings++;
        }

        public void dumpScores() {
            for(int i = 0; i < score.length; i++) {
                System.out.print(score[i] + " ");
            }
            System.out.println();
        }
    }

    private enum MovingOption {
        STOP,
        FORWARD,
        BACKWARD
    }

    private static class ShieldingOption {
        public final double angle;
        public final double power;
        public final double width;
        public final long breakTime;

        private ShieldingOption(double angle, double power, double width, long breakTime) {
            this.angle = angle;
            this.power = power;
            this.width = width;
            this.breakTime = breakTime;
        }
    }

    public void onKeyPressed(KeyEvent e) {}

    public void onPaint(Graphics2D gr) {
        if(!KeyConfig.getInstance().get('b'))
            return;

        G g = new G(gr);

        for(Wave wave : myWaves) {
            MyFireWave w = (MyFireWave) wave;
            g.drawRadial(w.getSource(), w.getAngle(), 0, w.getDistanceTraveled(robot.getTime()), Color.ORANGE);
        }

        if(movedAmount != null) {
            g.drawRadial(robot.getPoint(), Math.signum(movedAmount) * robot.getHeadingRadians(),
                    0, 50, Color.MAGENTA);
        }

//        if(lastWaves != null) {
//            for (Wave wave : lastWaves) {
//                EnemyFireWave w = (EnemyFireWave) wave;
//                g.drawRadial(w.getSource(), , 0, w.getDistanceTraveled(robot.getTime()), Color.ORANGE);
//            }
//        }
    }
}
