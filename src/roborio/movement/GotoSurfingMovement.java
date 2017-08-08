package roborio.movement;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.movement.forces.DangerPoint;
import roborio.movement.predictor.MovementPredictor;
import roborio.movement.predictor.PredictedPoint;
import roborio.movement.predictor.PredictedWaveImpact;
import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.myself.MySnapshot;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.AxisRectangle;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;
import roborio.utils.geo.Range;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.stats.Segmentation;
import roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 23/07/17
 * TODO: keep my distance
 * TODO: take bot-width into account when getting stats of a point (crucial to close range fighting)
 * TODO: surf 2 waves just for the sake of being able to reach the first wave spot in a speed which makes it easier to reach the second's
 * TODO: tweak the slices and try to add new crazy attributes
 * TODO: penalty for not moving (though flattening usually takes care of that, its worth it to penalize more)
 *
 * http://robowiki.net/wiki/Wave_Suffering
 */
public class GotoSurfingMovement extends Movement {
    private static final double             MAX_ESCAPE_ANGLE = 0.7;

    private static Segmentation<GuessFactorStats> stats, fastStats, flattening;
    private static double[] STATS_WEIGHTS = new double[]{0.2, 0.5, 0.3};
    private static int STATS_COUNT = 3;

    private static int FAST_STATS_ID = 0;
    private static int STATS_ID = 1;
    private static int FLATTENING_ID = 2;

    private static double[] EMPTY_SLICES = {Double.POSITIVE_INFINITY};

    static {
        fastStats = new Segmentation<>(new double[][] {
                {150.0, 400.0}, // distance
                {1.0, 4.0}, // lateralVelocity
                EMPTY_SLICES,
                EMPTY_SLICES,
                {1.25}, // bullet power
                EMPTY_SLICES
        });

        stats = new Segmentation<>(new double[][] {
                {150.0, 350.0, 550.0}, // distance
                {2.0, 4.0, 6.0}, // lateral velocity
                {-5.0, -3.0, -1.0, 2.0}, // advancing velocity
                {40.0, 140.0}, // wall distance
                {1.0, 1.5, 2.1}, // bullet power
                {-0.3, +0.3} // acceleration signed by direction
        });

        flattening = new Segmentation<>(new double[][] {
                {150.0, 450.0, 575.0},
                {1.5, 5.0},
                {3.0, 1.0, 2.0},
                {40.0, 140.0},
                {1.0, 1.5, 2.1},
                {-2.0, -0.3, +0.3}
        });
    }

    private WaveMap<GuessFactorStats[]>         waves;
    private HashMap<String, Double>           lastEnergy;

    private EnemyWave       _surfedWave;

    private AxisRectangle field;
    private EnemyLog        targetLog;
    private MyLog           myLog;
    private PowerGuesser    powerGuess;

    private ComplexEnemyRobot lastEnemy;

    /* used for panting */
    private static final int        WAVE_DIVISIONS = 81;

    private int                   _lastAwayDirection;
    private int                   _wavesPassed;
    private int                   _shotsTaken;
    private GuessFactorStats _lastStats;
    private DangerPoint           _gotoPoint;
    private List<Point>           _predicted;

    public GotoSurfingMovement(BackAsFrontRobot robot) {
        super(robot);
        waves = new WaveMap<>();
        field = robot.getBattleField();
        targetLog = null;
        myLog = MyLog.getInstance();
        powerGuess = new PowerGuesser(0.5);
        lastEnergy = new HashMap<>();

        _wavesPassed = 0;
        _shotsTaken = 0;
        _lastAwayDirection = 1;
    }

    private GuessFactorStats getStatsFromSegmentation(Segmentation<GuessFactorStats> segmentation, double[] values) {
        GuessFactorStats res = segmentation.get(values);
        if(res == null) {
            res = new GuessFactorStats(0.5);
            segmentation.add(values, res);
        }

        return res;
    }

    private GuessFactorStats[] getStats(double[] values) {
        return new GuessFactorStats[]{
                getStatsFromSegmentation(fastStats, values),
                getStatsFromSegmentation(stats, values),
                getStatsFromSegmentation(flattening, values)
        };
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if(lastEnergy.containsKey(e.getName())) {
            double newEnergy = lastEnergy.get(e.getName()) -
                    Rules.getBulletDamage(e.getBullet().getPower());
            lastEnergy.put(e.getName(), newEnergy);
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if(lastEnergy.containsKey(e.getName())) {
            double newEnergy = lastEnergy.get(e.getName())
                    + Rules.getBulletHitBonus(e.getPower());
            lastEnergy.put(e.getName(), newEnergy);
        }

        Point hitPoint = new Point(e.getBullet().getX(), e.getBullet().getY());

        Iterator<Wave> iterator = waves.iterator();

        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave instanceof EnemyFireWave) {
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

    private GuessFactorStats[] getStatsFromWave(Wave nextWave) {
        MyRobot me = nextWave.getSnapshot().getOffset(-1);
        MyRobot pastMe = nextWave.getSnapshot().getOffset(-2);
        double distance = nextWave.getSource().distance(me.getPoint());
        double lateralVelocity = me.getLateralVelocity(nextWave.getSource());
        double wallDistance = me.getDistanceToWall();
        double advVelocity = me.getApproachingVelocity(nextWave.getSource());
        double power = Physics.bulletPower(nextWave.getVelocity());
        double accel = Math.abs(me.getVelocity() - pastMe.getVelocity()) * me.getDirection(nextWave.getSource());

        GuessFactorStats[] sts = getStats(new double[]{distance, lateralVelocity, advVelocity, wallDistance, power, accel});
        GuessFactorStats[] clonedStats = new GuessFactorStats[]{
                (GuessFactorStats) (sts[FAST_STATS_ID].clone()),
                (GuessFactorStats) (sts[STATS_ID].clone()),
                sts[FLATTENING_ID]
        };

        return clonedStats;
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        targetLog = EnemyTracker.getInstance().getLog(e);
        lastEnemy = targetLog.getLatest();

        double energyDelta = lastEnergy.containsKey(e.getName()) ? lastEnergy.get(e.getName())
                : e.getEnergy();
        energyDelta = e.getEnergy() - energyDelta;

        lastEnergy.put(e.getName(), e.getEnergy());

        // shot detected
        if(R.nearOrBetween(-Physics.MAX_POWER, energyDelta, -Physics.MIN_POWER)) {
            powerGuess.push(-energyDelta);
            double velocity = Physics.bulletVelocity(-energyDelta);
            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime() - 1), velocity);

            waves.add(wave, getStatsFromWave(wave));
        } else {
            // flattening
            double power = 1.0;
            double velocity = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyWave wave = new EnemyWave(snap, targetLog.atLeastAt(getTime() - 1), velocity);

            waves.add(wave, getStatsFromWave(wave));
        }
    }

    private void checkHits() {
        Iterator<Wave> iterator = waves.iterator();

        while(iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if(wave.hasPassed(getRobot().getPoint(), getTime())) {
                this.log(wave, getRobot().getPoint());
                iterator.remove();
                _wavesPassed++;
                break;
            }
        }
    }

    @Override
    public void doMovement() {
        checkHits();

        Point currentLocation = myLog.getLatest().getPoint();
        final MyRobot my = myLog.getLatest();
        EnemyFireWave nextWave = (EnemyFireWave) waves.earliestWave(my.getPoint(), getTime(),
                new WaveCollection.EnemyFireWaveCondition() {
                    @Override
                    public boolean test(Wave wave) {
                        return super.test(wave) && !wave.hasPassed(my.getPoint(), my.getTime());
                    }
                });

        if(nextWave == null || nextWave.getEnemy().getPoint().distance(my.getPoint()) < 100) {
            _surfedWave = null;
            fallback();
            return;
        }

        if(_surfedWave == nextWave) {
            goTo(_gotoPoint);
            return;
        }

        _predicted = new ArrayList<Point>();

        ComplexEnemyRobot enemy = nextWave.getEnemy();

        GuessFactorStats[] sts = waves.getData(nextWave);
        GuessFactorStats st = mergeStats(sts);

//        double distance = getRobot().getPoint().distance(nextWave.getSource());
//        double mea = Physics.maxEscapeAngle(nextWave.getVelocity());
//        double bandwidth = Physics.hitAngle(distance) / (2.0*mea)
//                * GuessFactorStats.BUCKET_COUNT;
//
//        st.setSmoother(new GaussianSmoothing(bandwidth));

        DangerPoint dangerClockwise = getDanger(nextWave, +1, st);
        DangerPoint dangerCounterClockwise = getDanger(nextWave, -1, st);

        if(dangerClockwise.getDanger() < dangerCounterClockwise.getDanger()
                || getSteepness(dangerCounterClockwise, enemy.getPoint()) <= Math.toRadians(30)
                && getSteepness(dangerClockwise, enemy.getPoint()) > getSteepness(dangerCounterClockwise, enemy.getPoint())) {
            _gotoPoint = dangerClockwise;
        } else {
            _gotoPoint = dangerCounterClockwise;
        }

        _lastStats = st;
        _surfedWave = nextWave;

        goTo(_gotoPoint);
    }

    private void fallback() {
        if(targetLog != null) {
            MyRobot my = MyLog.getInstance().getLatest();
            ComplexEnemyRobot enemy = targetLog.getLatest();

            double distance = my.getPoint().distance(enemy.getPoint());
            double absBearing = Physics.absoluteBearing(enemy.getPoint(), my.getPoint());
            double offset = R.HALF_PI - 1 + distance / 400;

            AxisRectangle field = getRobot().getBattleField().shrink(18, 18);
            while(!field.contains(getRobot().getPoint().project(absBearing + offset * _lastAwayDirection, 160))) {
                offset += _lastAwayDirection*0.05;
            }

            setBackAsFront(absBearing + offset * _lastAwayDirection, 30);

            if(Math.abs(offset) > 4 * R.PI / 5)
                _lastAwayDirection *= -1;
        }
    }

    private double getGf(EnemyWave wave, Point hitPoint) {
        return getGf(wave, Physics.absoluteBearing(wave.getSource(), hitPoint));
    }

    private double getUnconstrainedGf(EnemyWave wave, Point hitPoint) {
        return getUnconstrainedGf(wave, Physics.absoluteBearing(wave.getSource(), hitPoint));
    }

    private double getUnconstrainedGf(EnemyWave wave, double absoluteBearing) {
        MySnapshot snap = wave.getSnapshot();
        MyRobot bot = snap.getOffset(-1); // -1 because enemy has aimed 1 tick before firing

        int myDirection = bot.getDirection(wave.getSource());

        double absoluteBearingWhenShot = Physics.absoluteBearing(wave.getSource(), bot.getPoint());
        double offsetAngle = Utils.normalRelativeAngle(absoluteBearing - absoluteBearingWhenShot);

        double factor = offsetAngle * myDirection /
                Physics.maxEscapeAngle(wave.getVelocity());

        return factor;
    }

    private double getGf(EnemyWave wave, double absoluteBearing) {
        return R.constrain(-1, getUnconstrainedGf(wave, absoluteBearing), +1);
    }

    private DangerPoint getDanger(EnemyFireWave wave, int direction, GuessFactorStats st) {
        ComplexEnemyRobot enemy = wave.getEnemy();
        double distance = getRobot().getPoint().distance(enemy.getPoint());

        PredictedPoint initialPoint = myLog.getLatest().getPredictionPoint();
        double absBearing = Physics.absoluteBearing(wave.getSource(), initialPoint);

        List<PredictedPoint> genPoints = MovementPredictor.predictOnWaveImpact(
                field, initialPoint,
                wave, direction, R.HALF_PI - (1 - (distance / 400)) * 0.7);

        if(genPoints.size() == 0)
            throw new IllegalStateException();

        if(genPoints.size() > 1) {
            genPoints.remove(genPoints.size() - 1); // last point is not feasible
        }

        double best = Double.POSITIVE_INFINITY;
        Point res = null;
        Point last = null;
        Point back = genPoints.get(genPoints.size() - 1);

        for(PredictedPoint predicted : genPoints) {
            if(last == null || predicted == back || last.distance(predicted) > 20) {
                last = predicted;

                PredictedWaveImpact data = MovementPredictor.preciselyPredictOnWaveImpact(
                        initialPoint,
                        wave,
                        predicted
                );

                Point impactPoint = data.getMidwayImpactPoint();
                _predicted.add(impactPoint);

                double impactGf = getGf(wave, impactPoint);

                Range gfRange = new Range();
                gfRange.push(getGf(wave, Utils.normalAbsoluteAngle(data.getIntersection().min + absBearing)));
                gfRange.push(getGf(wave, Utils.normalAbsoluteAngle(data.getIntersection().max + absBearing)));

                int iBucket = st.getBucket(gfRange.min);
                int jBucket = st.getBucket(gfRange.max);

                // TODO: use gauss smoothing over here
                double value = 0;
                for(int i = iBucket; i <= jBucket; i++) {
                    double gf = st.getGuessFactor(i);
                    value += st.getValueFromBucket(i) * ( 1.0 / (Math.pow(Math.abs(gf - impactGf) + 1.0, 2)));
                }

//                double value = st.getValue(gfRange.getCenter());

                if (best > value) {
                    best = value;
                    res = predicted;
                } else if(R.isNear(best, value)
                        && impactPoint.distance(initialPoint) > res.distance(initialPoint)) {
                    res = predicted;
                }
            }
        }

        return new DangerPoint(res, best);
    }

    private double getSteepness(Point dest, Point enemy) {
        Point my = getRobot().getPoint();
        double angleEnemy = Physics.absoluteBearing(my, enemy);
        double angleDest = Physics.absoluteBearing(my, dest);
        return Math.abs(Utils.normalRelativeAngle(angleDest - angleEnemy));
    }

    private void goTo(Point dest) {
        if(_surfedWave != null && _gotoPoint != null) {
            getRobot().moveWithBackAsFront(dest, getRobot().getPoint().distance(dest));
        }
    }

    private void log(EnemyWave wave, Point hitPoint) {
        MyRobot me = wave.getSnapshot().getOffset(-1);
        MyRobot pastMe = wave.getSnapshot().getOffset(-2);
        double distance = wave.getSource().distance(me.getPoint());
        double lateralVelocity = me.getLateralVelocity(wave.getSource());
        double wallDistance = me.getDistanceToWall();
        double advVelocity = me.getApproachingVelocity(wave.getSource());
        double power = Physics.bulletPower(wave.getVelocity());
        double accel = Math.abs(me.getVelocity() - pastMe.getVelocity()) * me.getDirection(wave.getSource());

        double gf = getGf(wave, hitPoint);
        GuessFactorStats[] sts = getStats(new double[]{distance, lateralVelocity, advVelocity, wallDistance, power, accel});

        // make sure flattening waves are only logged to flattening array
        if(!(wave instanceof EnemyFireWave)) {
            if(sts[FLATTENING_ID] != null)
                sts[FLATTENING_ID].logGuessFactor(gf);
        } else {
            for (int i = 0; i < STATS_COUNT; i++) {
                if (sts[i] != null)
                    sts[i].logGuessFactor(gf);
            }
        }
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        G g = new G(graphics);

        if(lastEnemy != null) {
            g.drawPoint(lastEnemy.getPoint(), 36, Color.WHITE);
        }

        if(_surfedWave != null && _gotoPoint != null) {
            g.drawPoint(_gotoPoint, 6.0, Color.GREEN);
            g.drawLine(getRobot().getPoint(), _gotoPoint, Color.WHITE);
        }

        if(_predicted != null) {
            for(Point point : _predicted) {
                g.drawPoint(point, 4.0, Color.GRAY);
            }
        }

        Wave earliestWave = waves.earliestFireWave(myLog.getLatest());

        for(Wave cur : waves) {
            if(!(cur instanceof EnemyFireWave))
                continue;

            EnemyFireWave wave = (EnemyFireWave) cur;

            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()),
                wave == earliestWave ? Color.WHITE : Color.BLUE);

            ComplexEnemyRobot enemy = wave.getEnemy();
            MyRobot me = wave.getSnapshot().getOffset(-1);

            GuessFactorStats[] sts = waves.getData(wave);
            GuessFactorStats st = mergeStats(sts);

            double angle = 0;
            double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
            double maxDanger = 0;

            DangerPoint[] dangerPoints = new DangerPoint[WAVE_DIVISIONS];

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                angle += ratio;

                Point hitPoint = wave.getSource().project(angle, wave.getDistanceTraveled(getTime()));
                double gf = getUnconstrainedGf(wave, hitPoint);

                if(!R.nearOrBetween(-1, gf, +1))
                    continue;

                double value = st.getValue(gf);
                dangerPoints[i] = new DangerPoint(hitPoint, value);
                maxDanger = Math.max(maxDanger, value);
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

    private GuessFactorStats mergeStats(GuessFactorStats[] sts) {
        return GuessFactorStats.merge(sts, STATS_WEIGHTS);
    }

    public void printLog() {
        System.out.println("Waves passed: " + _wavesPassed);
        System.out.println("Shots taken: " + _shotsTaken);
    }
}
