package roborio.movement;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.movement.forces.DangerPoint;
import roborio.movement.predictor.MovementPredictor;
import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.myself.MySnapshot;
import roborio.utils.*;
import roborio.utils.Point;
import roborio.utils.waves.EnemyFireWave;
import roborio.utils.waves.Wave;
import roborio.utils.waves.WaveCollection;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class GFSurfingMovement extends Movement {
    private static final int WALL_STICK = 140;
    private static final int BUCKET_COUNT = 41;
    private static final int BUCKET_MID = (BUCKET_COUNT - 1) / 2;

    private double[]        buckets;
    private WaveCollection  waves;
    private AxisRectangle   field;
    private EnemyLog        targetLog;
    private MyLog           myLog;

    /* used for panting */
    private static final int        WAVE_DIVISIONS = 81;

    private DangerPoint[]         _predicted;
    private int                   _wavesPassed;
    private int                   _shotsTaken;

    public GFSurfingMovement(BackAsFrontRobot robot) {
        super(robot);
        waves = new WaveCollection();
        buckets = new double[BUCKET_COUNT];
        field = robot.getBattleField().shrink(18, 18);
        targetLog = null;
        myLog = MyLog.getInstance();

        _wavesPassed = 0;
        _shotsTaken = 0;
    }

    // TODO: use *instanceof* here
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        Point hitPoint = new Point(e.getBullet().getX(), e.getBullet().getY());

        List<Wave> waveList = waves.getWaves();
        Iterator<Wave> iterator = waveList.iterator();

        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.isEnemyWave() && wave.isReal()) {
                EnemyFireWave enemyWave = (EnemyFireWave) wave;
                if(enemyWave.wasFiredBy(e.getBullet(), getTime())) {
                    this.log(enemyWave, hitPoint);
                    iterator.remove();
                    _shotsTaken++;
                    break;
                }
            }
        }
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        targetLog = EnemyTracker.getInstance().getLog(e);

        double energyDelta = targetLog.getEnergyDelta();

        // shot detected
        if(R.nearOrBetween(-Physics.MAX_POWER, energyDelta, -Physics.MIN_POWER)) {
            double velocity = Physics.bulletVelocity(-energyDelta);
            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime() - 1), velocity);
            waves.add(wave);
        }
    }

    @Override
    public void doMovement() {
        // clean unuseful waves
        _wavesPassed += waves.removePassed(myLog.getLatest());

        Point currentLocation = myLog.getLatest().getPoint();
        EnemyFireWave nextWave = (EnemyFireWave) waves.earliestFireWave(myLog.getLatest());

        if(nextWave == null) {
            return;
        }

        DangerPoint dangerClockwise = getDanger(nextWave, +1);
        DangerPoint dangerCounterClockwise = getDanger(nextWave, -1);

        _predicted = new DangerPoint[]{dangerClockwise, dangerCounterClockwise};

        double angle;
        if(dangerClockwise.getDanger() < dangerCounterClockwise.getDanger())
            angle = naiveWallSmoothing(currentLocation,
                    Physics.absoluteBearing(currentLocation, dangerClockwise),
                    1);
        else
            angle = naiveWallSmoothing(currentLocation,
                    Physics.absoluteBearing(currentLocation, dangerCounterClockwise),
                    -1);

        getRobot().setBackAsFront(angle);
    }

    private int getUnconstrainedBucket(Wave wave, Point hitPoint) {
        double absoluteBearing = Physics.absoluteBearing(wave.getSource(), hitPoint);

        MySnapshot snap = wave.getSnapshot();
        MyRobot bot = snap.getOffset(-1); // -1 because enemy has aimed 1 tick before the wave creation

        double absoluteBearingWhenShot = Physics.absoluteBearing(wave.getSource(), bot.getPoint());
        double offsetAngle = Utils.normalRelativeAngle(absoluteBearing - absoluteBearingWhenShot);

        double factor = offsetAngle /
                Physics.maxEscapeAngle(wave.getVelocity()) * wave.getMyDirection();

        double x = factor * BUCKET_MID + BUCKET_MID;
        return (int) x;
    }

    private int getBucket(Wave wave, Point hitPoint) {
        return R.constrain(0, getUnconstrainedBucket(wave, hitPoint), BUCKET_COUNT - 1);
    }

    private DangerPoint getDanger(EnemyFireWave wave, int direction) {
        Point impactPoint = MovementPredictor.predictOnWaveImpact(myLog.getLatest().getPredictionPoint(),
                wave, direction);

        return new DangerPoint(impactPoint, buckets[getBucket(wave, impactPoint)]);
    }

    private void log(EnemyFireWave wave, Point hitPoint) {
        int index = getBucket(wave, hitPoint);
        for(int i = 0; i < BUCKET_COUNT; i++) {
            buckets[i] += 1.0 / ((index - i) * (index - i) + 1);
        }
    }

    private double naiveWallSmoothing(Point source, double angle, int direction) {
        while(!field.contains(source.project(angle, WALL_STICK)))
            angle += direction*0.05;
        return angle;
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        G g = new G(graphics);

        if(_predicted != null) {
            for (int i = 0; i < _predicted.length; i++) {
                g.drawPoint(_predicted[i], 6.0, Color.WHITE);
                g.drawLine(getRobot().getPoint(), _predicted[i], Color.WHITE);
            }
        }

        for(Wave wave : waves.getWaves()) {
            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()));

            double angle = 0;
            double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
            double maxDanger = 0;

            DangerPoint[] dangerPoints = new DangerPoint[WAVE_DIVISIONS];

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                angle += ratio;

                Point hitPoint = wave.getSource().project(angle, wave.getDistanceTraveled(getTime()));
                int bucketIndex = getUnconstrainedBucket(wave, hitPoint);

                if(!R.isBetween(0, bucketIndex, BUCKET_COUNT - 1))
                    continue;

                dangerPoints[i] = new DangerPoint(hitPoint, buckets[bucketIndex]);
                maxDanger = Math.max(maxDanger, buckets[bucketIndex]);
            }

            if(R.isNear(maxDanger, 0)) continue;

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                if(dangerPoints[i] == null)
                    continue;

                Color dangerColor = G.getDangerColor(dangerPoints[i].getDanger() / maxDanger);
                g.drawCircle(dangerPoints[i], 3.0, dangerColor);
            }
        }
    }

    public void printLog() {
        System.out.println("Waves passed: " + _wavesPassed);
        System.out.println("Shots taken: " + _shotsTaken);
    }
}
