package roborio.gunning;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GunFireEvent;
import roborio.gunning.utils.PowerSelection;
import roborio.myself.MyLog;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;
import roborio.utils.geo.Range;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.stats.Segmentation;
import roborio.utils.stats.smoothing.GaussianSmoothing;
import roborio.utils.stats.smoothing.Smoothing;
import roborio.utils.storage.NamedStorage;
import roborio.utils.waves.MyFireWave;
import roborio.utils.waves.MyWave;
import roborio.utils.waves.Wave;
import roborio.utils.waves.WaveCollection;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 25/07/17.
 * TODO: check flattener and fast learning, something seems wrong with merge maybe?
 * TODO: aim with base on my estimated position on next tick
 */
public class GuessFactorGun extends AutomaticGun {
    private GunFireEvent lastFireEvent;
    private double       absFireAngle;
    private double       absFireVelocity;

    private GuessFactorStats _lastStats;
    private ComplexEnemyRobot _lastEnemy;
    private Range _lastGfRange;

    private Segmentation<GuessFactorStats> stats, fastStats, virtualStats;

    private static int STATS_COUNT = 3;
    private static double[] STATS_WEIGHTS = new double[]{0.5, 0.25, 0.25};
    private static int  VIRTUAL_STATS_ID = 2;

    private static double[][] statsSlices, fastStatsSlices, virtualStatsSlices;
    private static double[] EMPTY_SLICE = {Double.POSITIVE_INFINITY};

    static {
        statsSlices = new double[][]{
                {150.0, 275.0, 500.0}, // distance
                {1.0, 2.5, 4.5}, // lateral velocity
                EMPTY_SLICE,
                {40.0, 140.0}, // wall distance
                {1.0, 1.8, 2.7}, // bullet power
                {-0.3, +0.3} // acceleration signed by direction
        };

        fastStatsSlices = new double[][]{
                {150.0, 450.0},
                {1.0, 4.0},
                EMPTY_SLICE,
                {100.0},
                EMPTY_SLICE,
                EMPTY_SLICE
        };

        virtualStatsSlices = new double[][]{
                {150.0, 450.0}, // distance
                {1.0, 3.0, 4.5}, // lateral velocity
                EMPTY_SLICE,
                {40.0, 140.0}, // wall distance
                {1.0, 2.1}, // bullet power
                {-0.3, +0.3} // acceleration signed by direcetion
        };
    }

    private WaveCollection waves;
    private double rollingDepth;
    private double _wouldHit = 0;

    public GuessFactorGun(BackAsFrontRobot robot, double rollOff, boolean isVirtual) {
        this(robot, rollOff, isVirtual, "GuessFactorGun");
    }

    public GuessFactorGun(BackAsFrontRobot robot, double rollOff, boolean isVirtual, String storageHint) {
        super(robot, isVirtual);

        rollingDepth = rollOff;
        waves = new WaveCollection();

        _wouldHit = 0;

        NamedStorage storage = NamedStorage.getInstance();
        if(!storage.contains(storageHint)) {
            storage.add(storageHint, new Segmentation[]{
                    new Segmentation<GuessFactorStats>(statsSlices),
                    new Segmentation<GuessFactorStats>(fastStatsSlices),
                    new Segmentation<GuessFactorStats>(virtualStatsSlices)
            });
        }

        Segmentation[] segments = (Segmentation[]) storage.get(storageHint);
        stats = (Segmentation<GuessFactorStats>) segments[0];
        fastStats = (Segmentation<GuessFactorStats>) segments[1];
        virtualStats = (Segmentation<GuessFactorStats>) segments[2];
    }

    private GuessFactorStats getStatsFromSegmentation(Segmentation<GuessFactorStats> segmentation, double[] values) {
        GuessFactorStats res = segmentation.get(values);
        if(res == null) {
            res = new GuessFactorStats(rollingDepth);
            segmentation.add(values, res);
        }

        return res;
    }

    private GuessFactorStats[] getStats(double[] values) {
        return new GuessFactorStats[]{
                getStatsFromSegmentation(stats, values),
                getStatsFromSegmentation(fastStats, values),
                getStatsFromSegmentation(virtualStats, values)
        };
    }

    @Override
    public String getName() {
        return "GuessFactorGun";
    }

    @Override
    public void doGunning() {
        if(lastFireEvent == null) return;

        MyLog log = MyLog.getInstance();
        Wave wave = new MyWave(log, absFireVelocity);

        waves.add(wave);
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);

        double bulletPower = PowerSelection.naive(MyLog.getInstance().getLatest(), enemy, 0.0);

        checkHits(enemy);

        double amea = Physics.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));
        Range mea = new Range(-amea, +amea);

        double maxAbsoluteEscape = mea.maxAbsolute();
        Range gfRange = new Range(mea.min / maxAbsoluteEscape, mea.max / maxAbsoluteEscape);

        double distance = enemy.getDistance();
        double lateralVelocity = enemy.getLateralVelocity();
        double advancingVelocity = enemy.getApproachingVelocity();
        double distanceToWall = enemy.getDistanceToWall();
        double accel = Math.abs(enemy.getVelocity() - pastEnemy.getVelocity()) * pastEnemy.getDirection();

        GuessFactorStats[] allStats = getStats(new double[]{distance, lateralVelocity, advancingVelocity,
                                                distanceToWall, bulletPower, accel});

        GuessFactorStats stats = GuessFactorStats.merge(allStats, STATS_WEIGHTS);

        double bandwidth = Physics.hitAngle(distance) / (2.0*amea)
                * GuessFactorStats.BUCKET_COUNT;
        Smoothing smoother = new GaussianSmoothing(bandwidth);

        stats.setSmoother(smoother);

        int enemyDirection = enemy.getDirection();
        double bestGF = stats.getBestGuessFactor(gfRange);
        double bearingOffset = enemyDirection * bestGF * amea;

        _lastStats = stats;
        _lastEnemy = enemy;
        _lastGfRange = gfRange;

        absFireAngle = Utils.normalAbsoluteAngle(enemy.getAbsoluteBearing() + bearingOffset);
        absFireVelocity = Rules.getBulletSpeed(bulletPower);
        setFireTo(absFireAngle, bulletPower);
    }

    private void checkHits(ComplexEnemyRobot enemy) {
        _wouldHit = 0;

        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasTouchedRobot(enemy.getPoint(), getTime())) {
                ComplexEnemyRobot pastEnemy =
                        EnemyTracker.getInstance().getLog(enemy).atLeastAt(wave.getTime() - 1);
                ComplexEnemyRobot pastPastEnemy =
                        EnemyTracker.getInstance().getLog(enemy).atLeastAt(wave.getTime() - 2);

                int enemyDirection = pastEnemy.getDirection();
                double offset = Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), enemy.getPoint())
                        - pastEnemy.getAbsoluteBearing());
                double gf = R.constrain(-1, enemyDirection * offset / Physics.maxEscapeAngle(wave.getVelocity()), +1);

                double distance = pastEnemy.getDistance();
                double lateralVelocity = pastEnemy.getLateralVelocity();
                double advancingVelocity = pastEnemy.getApproachingVelocity();
                double wallDistance = pastEnemy.getDistanceToWall();
                double bulletPower = Physics.bulletPower(wave.getVelocity());
                double accel = Math.abs(pastEnemy.getVelocity() - pastPastEnemy.getVelocity()) * pastEnemy.getDirection();

                GuessFactorStats[] allStats = getStats(new double[]{distance, lateralVelocity, advancingVelocity,
                                                    wallDistance, bulletPower, accel});

                if(wave instanceof MyFireWave) {
                    MyFireWave fireWave = (MyFireWave) wave;
                    Point projection = fireWave.project(fireWave.getAngle(), getTime());
                    if(enemy.getHitBox().contains(projection)) {
                        _wouldHit = 1.0;
                    }
                }

                if(!(wave instanceof MyFireWave)) {
                    if(allStats[VIRTUAL_STATS_ID] != null)
                        allStats[VIRTUAL_STATS_ID].logGuessFactor(gf);
                } else {
                    for(int i = 0; i < STATS_COUNT; i++)
                        if(allStats[i] != null)
                            allStats[i].logGuessFactor(gf);
                }

                iterator.remove();
            }

        }
    }

    @Override
    public void onFire(GunFireEvent e) {
        lastFireEvent = e;
        Wave wave = new MyFireWave(MyLog.getInstance(), lastFireEvent.getAngle(), lastFireEvent.getVelocity());
        waves.add(wave);
    }

    @Override
    public double wouldHit() {
        return _wouldHit;
    }

    @Override
    public void onPaint(Graphics2D gr) {
        G g = new G(gr);

        if(_lastStats == null) return;

        double maxValue = _lastStats.get(_lastStats.getBestBucket());

        for(int i = 0; i < _lastStats.size(); i++) {
            double gf = _lastStats.getGuessFactor(i);
            if(!_lastGfRange.isNearlyContained(gf)) continue;
            double gfValue = _lastStats.get(i);

            double dangerPercent = Math.sqrt(gfValue / maxValue);
            double angle = _lastEnemy.getAbsoluteBearing() + gf * _lastGfRange.maxAbsolute() * _lastEnemy.getDirection();

            g.drawCircle(getRobot().getPoint().project(angle, 100), 3.0, G.getDangerColor(dangerPercent));
        }
    }
}
