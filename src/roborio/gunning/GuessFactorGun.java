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
public class GuessFactorGun extends AutomaticGun {
    private static final double MAX_ESCAPE_ANGLE = 0.7;

    private static final int    DISTANCE_SEGMENTS = 4;
    private static final int    LATERAL_SEGMENTS = 3;
    private static final int    ADVANCING_SEGMENTS = 3;
    private static final int    WALL_SEGMENTS = 2;
    private static final int    POWER_SEGMENTS = 4;
    private static GuessFactorStats[][][][] stats;

    private GunFireEvent lastFireEvent;
    private double       absFireAngle;
    private double       absFireVelocity;

    private GuessFactorStats _lastStats;
    private ComplexEnemyRobot _lastEnemy;
    private Range _lastGfRange;

    static {
        stats = new GuessFactorStats[DISTANCE_SEGMENTS][LATERAL_SEGMENTS][WALL_SEGMENTS][POWER_SEGMENTS];
    }

    private WaveCollection waves;

    public GuessFactorGun(BackAsFrontRobot robot, boolean isVirtual) {
        super(robot, isVirtual);
        waves = new WaveCollection();
    }

    public GuessFactorStats getStats(double distance, double lateralVelocity, double wallDistance, double bulletPower) {
        int distanceSegment = Math.min((int)(distance / 150), DISTANCE_SEGMENTS - 1);
        int lateralSegment = R.constrain(0,  (int)(lateralVelocity /  3 + (LATERAL_SEGMENTS - 1) / 2), LATERAL_SEGMENTS - 1);
        //int advancingSegment = R.constrain(0, (int)(advancingVelocity / 3 + (ADVANCING_SEGMENTS - 1) / 2), ADVANCING_SEGMENTS -1 );
        int wallSegment = Math.min((int)(wallDistance / 120), WALL_SEGMENTS - 1);
        int powerSegment = Math.min((int)(Math.sqrt((bulletPower - 0.1) / 2.9) * POWER_SEGMENTS), POWER_SEGMENTS - 1);
        if(!R.isBetween(0, distanceSegment,DISTANCE_SEGMENTS -1)
            || !R.isBetween(0, lateralSegment, LATERAL_SEGMENTS - 1)
                //|| !R.isBetween(0, advancingSegment, ADVANCING_SEGMENTS - 1)
                || !R.isBetween(0, wallSegment, WALL_SEGMENTS - 1)
                || !R.isBetween(0, powerSegment, POWER_SEGMENTS - 1))
            return null;

        if(stats[distanceSegment][lateralSegment][wallSegment][powerSegment] == null)
            stats[distanceSegment][lateralSegment][wallSegment][powerSegment] = new GuessFactorStats(0.05);

        return stats[distanceSegment][lateralSegment][wallSegment][powerSegment];
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

        double bulletPower = PowerSelection.naive(MyLog.getInstance().getLatest(), enemy, 0.0);

        checkHits(enemy);

        Range mea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
                enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(10), getRobot().getPoint(),
                        getTime(), Rules.getBulletSpeed(bulletPower)));

        double maxAbsoluteEscape = mea.maxAbsolute();
        Range gfRange = new Range(mea.min / maxAbsoluteEscape, mea.max / maxAbsoluteEscape);

        double distance = enemy.getDistance();
        double lateralVelocity = enemy.getLateralVelocity();
        // double advancingVelocity = enemy.getApproachingVelocity();
        double distanceToWall = enemy.getDistanceToWall();

        GuessFactorStats stats = getStats(distance, lateralVelocity, distanceToWall, bulletPower);

        double bestGF = stats.getBestGuessFactor(gfRange);
        double bearingOffset = bestGF * MAX_ESCAPE_ANGLE;

        _lastStats = stats;
        _lastEnemy = enemy;
        _lastGfRange = gfRange;

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

                double offset = Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), enemy.getPoint())
                        - pastEnemy.getAbsoluteBearing());
                double gf = R.constrain(-1, offset / MAX_ESCAPE_ANGLE, +1);

                double distance = pastEnemy.getDistance();
                double lateralVelocity = pastEnemy.getLateralVelocity();
                //double advancingVelocity = pastEnemy.getApproachingVelocity();
                double wallDistance = pastEnemy.getDistanceToWall();
                double bulletPower = Physics.bulletPower(wave.getVelocity());

                GuessFactorStats stats = getStats(distance, lateralVelocity, wallDistance, bulletPower);

                stats.logGuessFactor(gf, (wave instanceof MyFireWave) ? 2.0 : 1.0);
                iterator.remove();
            }
        }
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
