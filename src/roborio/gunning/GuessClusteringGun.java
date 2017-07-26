package roborio.gunning;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GuessFactorStats;
import roborio.gunning.utils.PowerSelection;
import roborio.movement.predictor.MovementPredictor;
import roborio.myself.MyLog;
import roborio.structures.KdTree;
import roborio.structures.WeightedManhattanKdTree;
import roborio.utils.*;
import roborio.utils.waves.MyFireWave;
import roborio.utils.waves.MyWave;
import roborio.utils.waves.Wave;
import roborio.utils.waves.WaveCollection;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class GuessClusteringGun extends AutomaticGun {
    private static final double     MAX_ESCAPE_ANGLE = 0.7;
    private static final int        DIMENSIONS = 6;
    private static final int        SIZE_LIMIT = 25000;
    private static final int        NEIGHBOURS = 24;
    // distance - lateralVelocity - advancingVelocity - wallDistance - travelTime - accel
    private static final double[]   WEIGHTS = new double[]{1.0, 100.0, 20.0, 1.0, 20.0, 0.0};

    private static KdTree<Double> tree;
    private WaveCollection waves;
    private GunFireEvent lastFireEvent;
    private double absFireAngle;
    private double absFireVelocity;

    /* for panting */
    private GuessFactorStats    _lastStats;
    private ComplexEnemyRobot   _lastEnemy;
    private Range               _lastGfRange;

    public GuessClusteringGun(BackAsFrontRobot robot, boolean isVirtual) {
        super(robot, isVirtual);
        if(tree == null)
            tree = new WeightedManhattanKdTree<Double>(WEIGHTS, SIZE_LIMIT);
        waves = new WaveCollection();
    }

    @Override
    public void doGunning() {
        if(lastFireEvent == null) return;

        MyLog log = MyLog.getInstance();
        Wave wave;
        if(lastFireEvent.getTime() == getTime())
            wave = new MyFireWave(log, lastFireEvent.getAngle(), lastFireEvent.getVelocity());
        else
            wave = new MyWave(log, absFireVelocity);

        waves.add(wave);
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(enemy.getTime() - 1);

        checkHits(enemy);
        double bulletPower = PowerSelection.naive(MyLog.getInstance().getLatest(), enemy, 0.0);

        double accel = computeAccel(pastEnemy, enemy);

        double[] query = new double[]{enemy.getDistance(), enemy.getLateralVelocity(),
                enemy.getApproachingVelocity(), enemy.getDistanceToWall(),
                getRobot().getPoint().distance(enemy.getPoint()) / Rules.getBulletSpeed(bulletPower),
                accel};


        double bestGF = 0;
        Range mea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
                enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(10), getRobot().getPoint(),
                        getTime(), Rules.getBulletSpeed(bulletPower)));

        double maxAbsoluteEscape = mea.maxAbsolute();
        Range gfRange = new Range(mea.min / maxAbsoluteEscape, mea.max / maxAbsoluteEscape);

        if (tree.size() > 0) {
            List<KdTree.Entry<Double>> entries = tree.kNN(query, NEIGHBOURS);
            GuessFactorStats stats = new GuessFactorStats(0);

            double minDist = Double.POSITIVE_INFINITY;
            double maxDist = Double.NEGATIVE_INFINITY;
            for(KdTree.Entry<Double> entry : entries) {
                minDist = Math.min(minDist, entry.distance);
                maxDist = Math.max(maxDist, entry.distance);
            }

            for(KdTree.Entry<Double> entry : entries) {
                double pointGF = entry.payload;
                double weight = Math.max((entry.distance - minDist) / (maxDist - minDist), 0.25);
                stats.logGuessFactor(pointGF, weight);
            }

            bestGF = stats.getBestGuessFactor(gfRange);

            _lastStats = stats;
            _lastEnemy = enemy;
        }

        _lastGfRange = gfRange;

        double bearingOffset = bestGF * maxAbsoluteEscape;

        absFireAngle = Utils.normalAbsoluteAngle(enemy.getAbsoluteBearing() + bearingOffset);
        absFireVelocity = Rules.getBulletSpeed(bulletPower);
        setFireTo(absFireAngle, bulletPower);
    }

    private void checkHits(ComplexEnemyRobot enemy) {
        List<Wave> waveList = waves.getWaves();
        Iterator<Wave> iterator = waveList.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasTouchedRobot(enemy.getPoint(), getTime())) {
                ComplexEnemyRobot pastEnemy =
                        EnemyTracker.getInstance().getLog(enemy).atLeastAt(wave.getTime() - 1);

                if(pastEnemy.getTime() + 1 != wave.getTime())
                    System.out.println("Couldnt go further back in log");

                ComplexEnemyRobot pastPastEnemy =
                        EnemyTracker.getInstance().getLog(enemy).atLeastAt(wave.getTime() - 2);

                double offset = Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), enemy.getPoint())
                        - pastEnemy.getAbsoluteBearing());
                double gf = R.constrain(-1, offset / MAX_ESCAPE_ANGLE, +1);

                double distance = pastEnemy.getDistance();
                double lateralVelocity = pastEnemy.getLateralVelocity();
                double advancingVelocity = pastEnemy.getApproachingVelocity();
                double wallDistance = pastEnemy.getDistanceToWall();
                double accel = computeAccel(pastPastEnemy, pastEnemy);

                tree.add(new double[]{distance, lateralVelocity, advancingVelocity, wallDistance, wave.getBreakTime(pastEnemy), accel}, gf);
                iterator.remove();
            }
        }
    }

    private double computeAccel(ComplexEnemyRobot pastPastEnemy, ComplexEnemyRobot pastEnemy) {
        double accel = (Math.abs(pastEnemy.getVelocity()) > Math.abs(pastPastEnemy.getVelocity()))
                ? 1
                : -2;
        if(R.isNear(pastEnemy.getVelocity(), pastPastEnemy.getVelocity()))
            accel = 0;
        return accel;
    }

    @Override
    public void onFire(GunFireEvent e) {
        lastFireEvent = e;
    }

    @Override
    public void onPaint(Graphics2D gr) {
        G g = new G(gr);

        if(_lastStats == null) return;

        double maxValue = _lastStats.buffer[_lastStats.getBestBucket()];

        for(int i = 0; i < _lastStats.buffer.length; i++) {
            double gf = _lastStats.getGuessFactor(i);
            if(!_lastGfRange.isNearlyContained(gf)) continue;
            double gfValue = _lastStats.buffer[i];

            double dangerPercent = Math.sqrt(gfValue / maxValue);
            double angle = _lastEnemy.getAbsoluteBearing() + gf * MAX_ESCAPE_ANGLE;

            g.drawCircle(getRobot().getPoint().project(angle, 100), 3.0, G.getDangerColor(dangerPercent));
        }
    }
}
