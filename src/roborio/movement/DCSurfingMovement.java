package roborio.movement;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GuessFactorRange;
import roborio.gunning.utils.LinearGuessFactorRange;
import roborio.gunning.utils.TargetingLog;
import roborio.movement.forces.DangerPoint;
import roborio.movement.predictor.MovementPredictor;
import roborio.movement.predictor.PredictedPoint;
import roborio.movement.predictor.PredictedWaveImpact;
import roborio.myself.MyLog;
import roborio.myself.MyRobot;
import roborio.myself.MySnapshot;
import roborio.structures.KdTree;
import roborio.structures.WeightedManhattanKdTree;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.AxisRectangle;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;
import roborio.utils.geo.Range;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.stats.smoothing.GaussianSmoothing;
import roborio.utils.storage.NamedStorage;
import roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 07/08/17.
 * TODO: fix my direction (use ahead = 1 / -1)
 * TODO: preciseMea seems to be wrong
 */
public class DCSurfingMovement extends Movement {
    static private double[] BASE_WEIGHTS =
            new double[]{1, 1, 1, 1, 1, 1, 1};

    private WaveMap<TargetingLog> waves;
    private AxisRectangle field;
    private MyLog myLog;
    private EnemyLog targetLog;
    private Double lastEnergy = null;

    private ArrayList<Wave> _surfedWaves;

    private long _bulletsFired = 0;

    private ArrayList<PredictedWaveImpact> _predicted;

    private long _wavesPassed = 0;
    private long _shotsTaken = 0;
    private Point _gotoPoint;

    private int                   _lastAwayDirection = 1;

    private KdTree<GuessFactorRange> tree, flatTree;

    public DCSurfingMovement(BackAsFrontRobot robot, String storageHint) {
        super(robot);
        waves = new WaveMap<>();
        field = robot.getBattleField();
        myLog = MyLog.getInstance();

        _surfedWaves = new ArrayList<Wave>();

        NamedStorage store = NamedStorage.getInstance();

        if(!store.contains(storageHint + "-tree")) {
            store.add(storageHint + "-tree", new WeightedManhattanKdTree<GuessFactorRange>(BASE_WEIGHTS, 3000));
        }

        if(!store.contains(storageHint + "-flat")) {
            store.add(storageHint + "-flat", new WeightedManhattanKdTree<GuessFactorRange>(BASE_WEIGHTS, 2000));
        }

        if(!store.contains(storageHint + "-fired")) {
            store.add(storageHint + "-fired", 0L);
        }

        _bulletsFired = (Long) store.get(storageHint + "-fired");

        tree = (KdTree<GuessFactorRange>) store.get(storageHint + "-tree");
        flatTree = (KdTree<GuessFactorRange>) store.get(storageHint + "-flat");
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if(lastEnergy == null)
            return;

        lastEnergy -= Rules.getBulletDamage(e.getBullet().getPower());
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if(lastEnergy == null)
            return;
        lastEnergy += Rules.getBulletHitBonus(e.getBullet().getPower());

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

    @Override
    public void onScan(ScannedRobotEvent e) {
        targetLog = EnemyTracker.getInstance().getLog(e);
        double energyDelta = e.getEnergy() - (lastEnergy == null ? e.getEnergy() : lastEnergy);

        lastEnergy = e.getEnergy();

        if(R.nearOrBetween(-Physics.MAX_POWER, energyDelta, -Physics.MIN_POWER)) {
            double power = -energyDelta;
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime() - 1), speed);

            waves.add(wave, getTargetingLog(wave));
        } else {
            double power = 1.0;
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyWave wave = new EnemyWave(snap, targetLog.atLeastAt(getTime() - 1), speed);

            waves.add(wave, getTargetingLog(wave));
        }
    }

    @Override
    public void doMovement() {
        checkHits();

        final MyRobot me = myLog.getLatest();
        EnemyFireWave nextWave = (EnemyFireWave) waves.earliestWave(me.getPoint() , getTime(), new WaveCollection.EnemyFireWaveCondition() {
            @Override
            public boolean test(Wave wave) {
                return super.test(wave) && !wave.hasPassed(me.getPoint(), me.getTime());
            }
        });

        if(nextWave == null) {
            _surfedWaves.clear();
            fallback();
            return;
        }

        if(!_surfedWaves.isEmpty() && _surfedWaves.get(0) == nextWave) {
            goTo(_gotoPoint);
            return;
        }

        TargetingLog log = waves.getData(nextWave);
        ComplexEnemyRobot enemy = nextWave.getEnemy();

        GuessFactorStats stats = getStats(log);
        Range preciseMea = log.getPreciseMea();

        double bandwidth = Physics.hitAngle(log.distance) / preciseMea.getLength()
                * GuessFactorStats.BUCKET_COUNT;
        stats.setSmoother(new GaussianSmoothing(bandwidth * 0.4));

        _predicted = new ArrayList<>();
        DangerPoint dangerClockwise = getDanger(nextWave, +1, stats, preciseMea);
        DangerPoint dangerCounterClockwise = getDanger(nextWave, -1, stats, preciseMea);

        if(dangerClockwise.getDanger() < dangerCounterClockwise.getDanger()) {
//                || getSteepness(dangerCounterClockwise, enemy.getPoint()) <= Math.toRadians(30)
//                && getSteepness(dangerClockwise, enemy.getPoint()) > getSteepness(dangerCounterClockwise, enemy.getPoint())) {
            _gotoPoint = dangerClockwise;
        } else {
            _gotoPoint = dangerCounterClockwise;
        }

        _surfedWaves = new ArrayList<>();
        _surfedWaves.add(nextWave);

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

    private void checkHits() {
        Iterator<Wave> iterator = waves.iterator();

        while (iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if (wave.hasPassed(getRobot().getPoint(), getTime())) {
                this.log(wave, getRobot().getPoint());
                iterator.remove();
                _wavesPassed++;
                break;
            }
        }
    }

    private void log(EnemyWave wave, Point point) {
        TargetingLog log = waves.getData(wave);
        double gf = getGf(wave, point, log.getPreciseMea());
        double[] location = getLocation(log);

        if(wave instanceof EnemyFireWave) {
            tree.add(location, new LinearGuessFactorRange(gf, gf));
        }

        flatTree.add(location, new LinearGuessFactorRange(gf, gf));
    }

    private DangerPoint getDanger(EnemyFireWave wave, int clockDirection, GuessFactorStats stats, Range preciseMea) {
        PredictedPoint initialPoint = myLog.getLatest().getPredictionPoint();
        double absBearing = Physics.absoluteBearing(wave.getSource(), initialPoint);

        double distance = initialPoint.distance(wave.getSource());

        List<PredictedPoint> genPoints = MovementPredictor.predictOnWaveImpact(
                field, initialPoint,
                wave, clockDirection, R.HALF_PI - (1 - (distance / 400)) * 0.7);

        if(genPoints.size() == 0)
            throw new IllegalStateException();

        if(genPoints.size() > 1) {
            genPoints.remove(genPoints.size() - 1); // last point is not feasible
        }

        double best = Double.POSITIVE_INFINITY;
        PredictedPoint back = genPoints.get(genPoints.size() - 1);
        PredictedPoint last = null;
        PredictedPoint res = null;

        for(PredictedPoint predicted : genPoints) {
            if(last == null || predicted == back || last.distance(predicted) > 20) {
                last = predicted;

                PredictedWaveImpact data = MovementPredictor.preciselyPredictOnWaveImpact(
                        initialPoint,
                        wave,
                        predicted
                );

                _predicted.add(data);

                Point impactPoint = data.getMidwayImpactPoint();
                double impactGf = getGf(wave, impactPoint, preciseMea);

                Range gfRange = new Range();
                gfRange.push(getGf(wave, Utils.normalAbsoluteAngle(data.getIntersection().min + absBearing), preciseMea));
                gfRange.push(getGf(wave, Utils.normalAbsoluteAngle(data.getIntersection().max + absBearing), preciseMea));

                int iBucket = stats.getBucket(gfRange.min);
                int jBucket = stats.getBucket(gfRange.max);

                // TODO: use gauss smoothing over here
                double value = 0;
                for(int i = iBucket; i <= jBucket; i++) {
                    double gf = stats.getGuessFactor(i);
                    value += stats.getValueFromBucket(i) * ( 1.0 / (Math.pow(Math.abs(gf - impactGf) + 1.0, 2)));
                }

//                double value = stats.getValue(impactGf);

                if(value < best || R.isNear(best, value) && predicted.distance(initialPoint) > res.distance(initialPoint)) {
                    best = value;
                    res = predicted;
                }
            }
        }

        return new DangerPoint(res, best);
    }

    private double getUnconstrainedGf(Wave wave, double absBearing, Range preciseMea) {
        MyRobot me = wave.getSnapshot().getOffset(-1);
        int direction = me.getDirection(wave.getSource());

        double absBearingWhenShot = Physics.absoluteBearing(wave.getSource(), me.getPoint());
        double offset = Utils.normalRelativeAngle(absBearing - absBearingWhenShot);

        if(offset >= 0)
            return offset * direction / preciseMea.max;
        else
            return -offset * direction / preciseMea.min;
    }

    private double getGf(Wave wave, double absBearing, Range preciseMea) {
        return R.constrain(-1, getUnconstrainedGf(wave, absBearing, preciseMea), +1);
    }

    private double getGf(Wave wave, Point impactPoint, Range preciseMea) {
        return getGf(wave, Physics.absoluteBearing(wave.getSource(), impactPoint), preciseMea);
    }

    private void goTo(Point dest) {
        if(!_surfedWaves.isEmpty() && _gotoPoint != null) {
            getRobot().moveWithBackAsFront(dest, getRobot().getPoint().distance(dest));
        }
    }

    private double[] getLocation(TargetingLog log) {
        return new double[]{
                log.distance / Rules.getBulletSpeed(log.bulletPower) / 80,
                log.lateralVelocity / 8,
                log.accel,
                log.bulletPower,
                Math.pow(0.5*log.bulletsFired, 1.15),
                log.positiveEscape,
                log.negativeEscape
        };
    }

    private GuessFactorStats getStatsFromTree(KdTree<GuessFactorRange> t, double[] location, int K) {
        GuessFactorStats res = new GuessFactorStats(Double.POSITIVE_INFINITY);
        List<KdTree.Entry<GuessFactorRange>> entries = t.kNN(location, Math.min((int) R.sqrt(t.size()), K));

        double distSum = 1e-8;
        for(KdTree.Entry<GuessFactorRange> entry : entries) {
            distSum += entry.distance;
        }

        double invAvg = entries.size() / distSum;

        for(KdTree.Entry<GuessFactorRange> entry : entries) {
            double distance = entry.distance;
            double gf = entry.payload.mean;

            double x = distance * invAvg;
            double weight = R.exp(-0.5 * x * x);

            res.logGuessFactor(gf, weight);
        }

        return res;
    }

    private GuessFactorStats getStats(TargetingLog log) {
        double[] location = getLocation(log);

        GuessFactorStats treeStats = getStatsFromTree(tree, location, 48);
        GuessFactorStats flatStats = getStatsFromTree(tree, location, 24);

        return GuessFactorStats.merge(new GuessFactorStats[]{treeStats, flatStats},
                new double[]{0.65, 0.35});
    }

    private double getSteepness(Point dest, Point enemy) {
        Point my = getRobot().getPoint();
        double angleEnemy = Physics.absoluteBearing(my, enemy);
        double angleDest = Physics.absoluteBearing(my, dest);
        return Math.abs(Utils.normalRelativeAngle(angleDest - angleEnemy));
    }

    private TargetingLog getTargetingLog(EnemyWave wave) {
        double bulletSpeed = wave.getVelocity();
        double bulletPower = Physics.bulletPower(bulletSpeed);

        MyRobot me = wave.getSnapshot().getOffset(-1);
        MyRobot pastMe = wave.getSnapshot().getOffset(-2);

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(
                field,
                me.getPredictionPoint(),
                wave,
                me.getDirection(wave.getSource())
        );

        double mea = Physics.maxEscapeAngle(bulletSpeed);

        TargetingLog log = new TargetingLog();
        log.source = wave.getSource();
        log.direction = me.getDirection(wave.getSource()); // improve this?
        log.absBearing = wave.getAngle(me.getPoint());
        log.lateralVelocity = me.getLateralVelocity(wave.getSource());
        log.bulletPower = bulletPower;
        log.bulletsFired = _bulletsFired;
        log.distance = me.getPoint().distance(wave.getSource());
        log.accel = (me.getVelocity() - pastMe.getVelocity())
                * Math.signum(me.getVelocity() + 1e-8);
        log.positiveEscape = Math.abs(preciseMea.max / mea);
        log.negativeEscape = Math.abs(preciseMea.min / mea);

        return log;
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        final int WAVE_DIVISIONS = 71;

        G g = new G(graphics);

        if(_predicted != null) {
            for (PredictedWaveImpact data : _predicted) {
                g.drawPoint(data.getMidwayImpactPoint(), 8.0, Color.RED);
            }
        }

        if(!_surfedWaves.isEmpty() && _gotoPoint != null) {
            g.drawPoint(_gotoPoint, 6.0, Color.GREEN);
            g.drawLine(getRobot().getPoint(), _gotoPoint, Color.WHITE);
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

            TargetingLog log = waves.getData(wave);
            Range preciseMea = log.getPreciseMea();

            GuessFactorStats st = getStats(log);
            double bandwidth = Physics.hitAngle(log.distance) / preciseMea.getLength()
                    * GuessFactorStats.BUCKET_COUNT;
            st.setSmoother(new GaussianSmoothing(bandwidth * 0.4));

            double angle = 0;
            double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
            double maxDanger = 0;

            DangerPoint[] dangerPoints = new DangerPoint[WAVE_DIVISIONS];

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                angle += ratio;

                Point hitPoint = wave.getSource().project(angle, wave.getDistanceTraveled(getTime()));
                double gf = getUnconstrainedGf(wave, angle, preciseMea);

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

                Color dangerColor = G.getSafeColor(dangerPoints[i].getDanger() / maxDanger);
                g.drawCircle(dangerPoints[i], 3.0, dangerColor);
            }
        }
    }

    public void printLog() {
        System.out.println("Waves passed: " + _wavesPassed);
        System.out.println("Shots taken: " + _shotsTaken);
    }
}
