package roborio.movement;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GuessFactorStats;
import roborio.movement.forces.DangerPoint;
import roborio.movement.predictor.MovementPredictor;
import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.myself.MySnapshot;
import roborio.utils.*;
import roborio.utils.Point;
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
 * TODO: improve my stats, the smoothing functions, rolling averages and that stuff
 * TODO: explicitly distinguish flattening buffer from the firing buffer
 *
 * http://robowiki.net/wiki/Wave_Suffering
 */
public class GotoSurfingMovement extends Movement {
    private static final int                BUCKET_COUNT = 31;
    private static final int                BUCKET_MID = (BUCKET_COUNT - 1) / 2;
    private static final double             MAX_ESCAPE_ANGLE = 0.7;
    private static final int                DISTANCE_SEGMENTS = 6;
    private static final int                LATERAL_SEGMENTS = 4;
    private static final int                WALL_SEGMENTS = 2;
    private static GuessFactorStats[][][]   stats;

    static {
        stats = new GuessFactorStats[DISTANCE_SEGMENTS][LATERAL_SEGMENTS][WALL_SEGMENTS];
    }

    private WaveMap<GuessFactorStats>         waves;
    private HashMap<String, Double>           lastEnergy;

    private EnemyWave       _surfedWave;

    private AxisRectangle   field;
    private EnemyLog        targetLog;
    private MyLog           myLog;
    private PowerGuesser    powerGuess;

    /* used for panting */
    private static final int        WAVE_DIVISIONS = 81;

    private int                   _lastAwayDirection;
    private int                   _wavesPassed;
    private int                   _shotsTaken;
    private GuessFactorStats      _lastStats;
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

    private GuessFactorStats getStats(double distance, double lateralVelocity, double wallDistance) {
        int distanceSegment = Math.min((int)(distance / 300), DISTANCE_SEGMENTS - 1);
        int lateralSegment = R.constrain(0,  (int)(Math.abs(lateralVelocity / 2)), LATERAL_SEGMENTS - 1);
        int wallSegment = wallDistance <= 120 ? 0 : 1;
        if(!R.isBetween(0, distanceSegment,DISTANCE_SEGMENTS -1)
                || !R.isBetween(0, lateralSegment, LATERAL_SEGMENTS - 1)
                //|| !R.isBetween(0, advancingSegment, ADVANCING_SEGMENTS - 1)
                || !R.isBetween(0, wallSegment, WALL_SEGMENTS - 1))
            return null;

        if(stats[distanceSegment][lateralSegment][wallSegment] == null)
            stats[distanceSegment][lateralSegment][wallSegment] = new GuessFactorStats(0.03, 1.0);

        return stats[distanceSegment][lateralSegment][wallSegment];
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

        List<Wave> waveList = waves.getWaves();
        Iterator<Wave> iterator = waveList.iterator();

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

    private GuessFactorStats getStatsFromWave(Wave nextWave) {
        MyRobot me = nextWave.getSnapshot().getOffset(-1);
        double distance = nextWave.getSource().distance(me.getPoint());
        double lateralVelocity = me.getLateralVelocity(nextWave.getSource());
        double wallDistance = me.getDistanceToWall();

        return getStats(distance, lateralVelocity, wallDistance);
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        targetLog = EnemyTracker.getInstance().getLog(e);

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

            waves.add(wave, (GuessFactorStats) getStatsFromWave(wave).clone());
        } else {
            // flattening
            double power = powerGuess.getGuess();
            double velocity = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyWave wave = new EnemyWave(snap, targetLog.atLeastAt(getTime() - 1), velocity);

            waves.add(wave, (GuessFactorStats) getStatsFromWave(wave).clone());
        }
    }

    private void checkHits() {
        List<Wave> waveList = waves.getWaves();
        Iterator<Wave> iterator = waveList.iterator();

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
        MyRobot my = myLog.getLatest();
        EnemyFireWave nextWave = (EnemyFireWave) waves.earliestWave(my.getPoint(), getTime(),
                new WaveCollection.EnemyFireWaveCondition() {
                    @Override
                    public boolean test(Wave wave) {
                        return super.test(wave) && !wave.hasTouchedRobot(my.getPoint(), my.getTime());
                    }
                });

        if(nextWave == null || nextWave.getEnemy().getPoint().distance(my.getPoint()) < 60) {
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

        GuessFactorStats st = waves.getData(nextWave);
//        MyRobot me = nextWave.getSnapshot().getOffset(-1);
//        GuessFactorStats st = getStats(me.getPoint().distance(enemy.getPoint()),
//                            me.getLateralVelocity(enemy.getPoint()), me.getDistanceToWall());

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

    private double getUnconstrainedGf(EnemyWave wave, Point hitPoint) {
        double absoluteBearing = Physics.absoluteBearing(wave.getSource(), hitPoint);

        MySnapshot snap = wave.getSnapshot();
        MyRobot bot = snap.getOffset(-1); // -1 because enemy has aimed 1 tick before firing

        int myDirection = bot.getDirection(wave.getSource());

        double absoluteBearingWhenShot = Physics.absoluteBearing(wave.getSource(), bot.getPoint());
        double offsetAngle = Utils.normalRelativeAngle(absoluteBearing - absoluteBearingWhenShot);

        double factor = offsetAngle * myDirection /
                Physics.maxEscapeAngle(wave.getVelocity());

        return factor;
    }

    private double getGf(EnemyWave wave, Point hitPoint) {
        return R.constrain(-1, getUnconstrainedGf(wave, hitPoint), +1);
    }

    private DangerPoint getDanger(EnemyFireWave wave, int direction, GuessFactorStats st) {
        ComplexEnemyRobot enemy = wave.getEnemy();
        double distance = getRobot().getPoint().distance(enemy.getPoint());

        MovementPredictor.PredictedPoint initialPoint = myLog.getLatest().getPredictionPoint();

        List<MovementPredictor.PredictedPoint> genPoints = MovementPredictor.predictOnWaveImpact(
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

        for(MovementPredictor.PredictedPoint predicted : genPoints) {
            if(last == null || predicted == back || last.distance(predicted) > 20) {
                last = predicted;
                List<MovementPredictor.PredictedPoint> possiblePoints = MovementPredictor.predictOnWaveImpact(
                        myLog.getLatest().getPredictionPoint(),
                        wave,
                        predicted);

                Point impactPoint = possiblePoints.get(possiblePoints.size() - 1);
                _predicted.add(impactPoint);

                double value = st.getValue(getGf(wave, impactPoint));
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
        double distance = wave.getSource().distance(me.getPoint());
        double lateralVelocity = me.getLateralVelocity(wave.getSource());
        double wallDistance = me.getDistanceToWall();

        double gf = getGf(wave, hitPoint);

        GuessFactorStats stats = getStats(distance, lateralVelocity, wallDistance);
        if(stats != null)
            stats.logGuessFactor(gf);
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        G g = new G(graphics);

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

        for(Wave cur : waves.getWaves()) {
            if(!(cur instanceof EnemyFireWave))
                continue;

            EnemyFireWave wave = (EnemyFireWave) cur;

            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()),
                wave == earliestWave ? Color.WHITE : Color.BLUE);

            ComplexEnemyRobot enemy = wave.getEnemy();
            MyRobot me = wave.getSnapshot().getOffset(-1);

            GuessFactorStats st = waves.getData(wave);

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

    public void printLog() {
        System.out.println("Waves passed: " + _wavesPassed);
        System.out.println("Shots taken: " + _shotsTaken);
    }
}
