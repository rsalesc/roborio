package rsalesc.roborio.movement;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.LinearGuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.movement.forces.DangerPoint;
import rsalesc.roborio.movement.forces.DangerWavePoint;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.movement.predictor.PredictedWaveImpact;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.structures.KdTree;
import rsalesc.roborio.structures.WeightedManhattanKdTree;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 07/08/17]
 * TODO: bot is somehow not going to the safest spot in the wave
 */
public class DCSurfingMovement extends Movement {
    static private double[] BASE_WEIGHTS =
            new double[]{2, 3.5, 2.5, 1, 0.5, 1.25, 0.5};

    static private double[] WAVE_WEIGHTS =
            new double[]{0.85, 0.15};

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

    private ShadowManager shadowing;

    private WaveMap<TargetingLog> waves;
    private AxisRectangle field;
    private MyLog myLog;
    private EnemyLog targetLog;
    private Double lastEnergy = null;

    private ArrayList<Wave> _surfedWaves;

    private long _bulletsFired = 0;

    private ArrayList<Point> _predicted;

    private long _wavesPassed = 0;
    private long _shotsTaken = 0;
    private Point _gotoPoint;

    private int _lastAwayDirection = 1;

    private KdTree<GuessFactorRange> tree, flatTree;

    public DCSurfingMovement(BackAsFrontRobot robot, String storageHint) {
        super(robot);
        waves = new WaveMap<>();
        field = robot.getBattleField();
        myLog = MyLog.getInstance();
        shadowing = new ShadowManager();

        _surfedWaves = new ArrayList<Wave>();

        NamedStorage store = NamedStorage.getInstance();

        if(!store.contains(storageHint + "-knn")) {
            store.add(storageHint + "-knn", new WeightedManhattanKdTree<GuessFactorRange>(BASE_WEIGHTS, 6000));
        }

        if(!store.contains(storageHint + "-flat")) {
            store.add(storageHint + "-flat", new WeightedManhattanKdTree<GuessFactorRange>(BASE_WEIGHTS, 1500));
        }

        if(!store.contains(storageHint + "-fired")) {
            store.add(storageHint + "-fired", 0L);
        }

        _bulletsFired = (Long) store.get(storageHint + "-fired");

        tree = (KdTree<GuessFactorRange>) store.get(storageHint + "-knn");
        flatTree = (KdTree<GuessFactorRange>) store.get(storageHint + "-flat");
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;
        if(lastEnergy == null)
            return;

        lastEnergy -= Rules.getBulletDamage(e.getBullet().getPower());
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;

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
                    this.log(enemyWave, hitPoint, 3);
                    iterator.remove();
                    _shotsTaken++;
                    break;
                }
            }
        }
    }

    @Override
    public void doShadowing(VirtualBullet[] bullets) {
        shadowing.push(bullets);
        ArrayList<Wave> fireWaves = new ArrayList<>();
        for(Wave wave : waves) {
            if(wave instanceof EnemyFireWave)
                fireWaves.add(wave);
        }

        shadowing.push(fireWaves.toArray(new Wave[0]));
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;
        clearLastFire();

        targetLog = EnemyTracker.getInstance().getLog(e);
        double energyDelta = e.getEnergy() - (lastEnergy == null ? e.getEnergy() : lastEnergy);

        lastEnergy = e.getEnergy();

        if(R.nearOrBetween(-Physics.MAX_POWER, energyDelta, -Physics.MIN_POWER)) {
            double power = -energyDelta;
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime() - 1), speed);

            waves.add(wave, getTargetingLog(wave));

            onFire(targetLog.getLatest(), power);
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
        Wave[] _nextWaves = waves.earliestWaves(2, me.getPoint() , getTime(), new WaveCollection.EnemyFireWaveCondition() {
            @Override
            public boolean test(Wave wave) {
                return super.test(wave) && !wave.hasPassed(me.getPoint(), me.getTime());
            }
        });

        EnemyFireWave[] nextWaves = new EnemyFireWave[_nextWaves.length];
        for(int i = 0; i < _nextWaves.length; i++)
            nextWaves[i] = (EnemyFireWave) _nextWaves[i];

        if(nextWaves.length == 0) {
            _surfedWaves.clear();
            fallback();
            return;
        }

        if(!_surfedWaves.isEmpty() && _surfedWaves.size() == nextWaves.length &&
                _surfedWaves.get(0) == nextWaves[0]) {
            goTo(_gotoPoint);
            return;
        }

        GuessFactorStats[] sts = new GuessFactorStats[nextWaves.length];
        TargetingLog[] logs = new TargetingLog[nextWaves.length];
        for(int i = 0; i < sts.length; i++) {
            logs[i] = waves.getData(nextWaves[i]);
            sts[i] = getStats(logs[i]);

            double bandwidth = Physics.hitAngle(logs[i].distance) / logs[i].getPreciseMea().getLength()
                    * GuessFactorStats.BUCKET_COUNT * 0.8;
            sts[i].setSmoother(new GaussianSmoothing(bandwidth * 0.6));
        }

        _predicted = new ArrayList<Point>();
        _gotoPoint = surfTwo(nextWaves, sts, logs);

        _surfedWaves = new ArrayList<>();
        for(EnemyFireWave wave : nextWaves)
            _surfedWaves.add(wave);

        goTo(_gotoPoint);
    }

    private DangerPoint surfTwo(EnemyFireWave[] nextWaves, GuessFactorStats[] sts, TargetingLog[] logs) {
        EnemyFireWave nextWave = nextWaves[0];
        GuessFactorStats stats = sts[0];
        TargetingLog log = logs[0];

        double[] mx = new double[sts.length];
        for(int i = 0; i < sts.length; i++) {
            mx[i] = 1e-8;
            for(int j = 0; j < GuessFactorStats.BUCKET_COUNT; j++)
                mx[i] = Math.max(mx[i], sts[i].get(j));
            mx[i] = i >= WAVE_WEIGHTS.length ? 0 : WAVE_WEIGHTS[i] / mx[i];
        }

        Range preciseMea = log.getPreciseMea();

        DangerWavePoint[] candidates =
                getSurfingCandidates(myLog.getLatest().getPredictionPoint(), nextWave, stats, preciseMea, false);
        DangerPoint best = null;
        Point[] predicted = new Point[0];

        for(DangerWavePoint c1 : candidates) {
            double value = c1.getDanger() * mx[0];
            if(best != null && best.getDanger() < value)
                break;

            if(nextWaves.length > 1) {
                Range preciseMea2 = logs[1].getPreciseMea();

                DangerWavePoint[] futureCandidates =
                        getSurfingCandidates(c1.getImpact(), nextWaves[1], sts[1], preciseMea2, true);

                for (DangerWavePoint c2 : futureCandidates) {
                    double nextValue = Math.max(c1.getDanger() * mx[0] + c2.getDanger() * mx[1], value);
                    if(best != null && best.getDanger() < nextValue)
                        break;

                    value = nextValue;

                    if (best == null || best.getDanger() > value) {
                        best = new DangerPoint(c1, value);
                        predicted = new Point[]{c1, c2};
                    }
                }
            } else {
                if (best == null || best.getDanger() > value) {
                    best = new DangerPoint(c1, value);
                    predicted = new Point[]{c1};
                }
            }
        }

        for(Point point : predicted) {
            _predicted.add(point);
        }

        return best;
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
                this.log(wave, getRobot().getPoint(), 1);
                iterator.remove();
                _wavesPassed++;
            }
        }
    }

    private void log(EnemyWave wave, Point point, int weight) {
        TargetingLog log = waves.getData(wave);
        double gf = getGf(wave, point, log.getPreciseMea());
        double[] location = getLocation(log);

        if(wave instanceof EnemyFireWave) {
            for(int i = 0; i < weight; i++)
                tree.add(location, new LinearGuessFactorRange(gf, gf));
        }

        flatTree.add(location, new LinearGuessFactorRange(gf, gf));
    }

    private DangerWavePoint[] getSurfingCandidates(PredictedPoint initialPoint,
                                             EnemyFireWave wave, GuessFactorStats stats, Range preciseMea,
                                                   boolean fast) {
        MyRobot me = wave.getSnapshot().getOffset(-1);
        double absBearing = Physics.absoluteBearing(wave.getSource(), me.getPoint());
        double initialBearing = Physics.absoluteBearing(wave.getSource(), initialPoint);

        double distance = initialPoint.distance(wave.getSource());
        double perp = R.HALF_PI - (1 - (distance / 400)) * 0.7;

        List<PredictedPoint> genPoints = MovementPredictor.predictOnWaveImpact(
                field, initialPoint,
                wave, +1, perp);

        List<PredictedPoint> otherPoints = MovementPredictor.predictOnWaveImpact(
                field, initialPoint,
                wave, -1, perp);

        Collections.reverse(genPoints);
        genPoints.add(initialPoint);
        genPoints.addAll(otherPoints);


        if(genPoints.size() == 0)
            throw new IllegalStateException();

        ArrayList<DangerWavePoint> dangers = new ArrayList<>();
        PredictedPoint first = genPoints.get(0);
        PredictedPoint back = genPoints.get(genPoints.size() - 1);
        PredictedPoint last = null;

        int step = fast ? 30 : 20;

        ArrayList<Shadow> shadows = shadowing.getShadows(wave);

        for(PredictedPoint predicted : genPoints) {
            if(last == null || last == first || last == back || last.distance(predicted) > step) {
                last = predicted;

                // TODO: use gauss smoothing over here
                double value = 0;

                if(!fast) {
                    PredictedWaveImpact data = MovementPredictor.preciselyPredictOnWaveImpact(
                            initialPoint,
                            wave,
                            predicted
                    );

                    Point impactPoint = data.getMidwayImpactPoint();
                    double impactGf = getGf(wave, impactPoint, preciseMea);

                    Range gfRange = new Range();
                    double absMin = Utils.normalAbsoluteAngle(data.getIntersection().min + initialBearing);
                    double absMax = Utils.normalAbsoluteAngle(data.getIntersection().max + initialBearing);
                    gfRange.push(getGf(wave, absMin, preciseMea));
                    gfRange.push(getGf(wave, absMax, preciseMea));

                    int iBucket = stats.getBucket(gfRange.min);
                    int jBucket = stats.getBucket(gfRange.max);
                    for (int i = iBucket; i <= jBucket; i++) {
                        double gf = stats.getGuessFactor(i);
                        boolean usedShadow = false;
                        double angle = (gf >= 0 ? gf * preciseMea.max : -gf * preciseMea.min) + absBearing;

                        for(Shadow shadow : shadows) {
                            if(shadow.isInside(angle)) {
                                // add a length requirement?
                                usedShadow = true;
                                break;
                            }
                        }

                        if(usedShadow)
                            continue;

                        value += stats.getValueFromBucket(i) * (1.0 / (Math.pow(Math.abs(gf - impactGf) + 1.0, 1.2)));
                    }

                    value /= jBucket - iBucket + 1;

                    dangers.add(new DangerWavePoint(predicted, value, data.getMidwayImpactPoint()));
                } else {
                    List<PredictedPoint> points = MovementPredictor.predictOnWaveImpact(
                            initialPoint,
                            wave,
                            predicted
                    );

                    points.add(0, initialPoint);

                    PredictedPoint impactPoint = points.get(points.size() - 1);
                    double impactGf = getGf(wave, impactPoint, preciseMea);
                    value = stats.getValue(impactGf);

                    dangers.add(new DangerWavePoint(predicted, value, impactPoint));
                }

//                if(value < best || R.isNear(best, value) && predicted.distance(initialPoint) > res.distance(initialPoint)) {
//                    best = value;
//                    res = predicted;
//                }
            }
        }

        Collections.sort(dangers);
        return dangers.toArray(new DangerWavePoint[0]);
    }

    private double getUnconstrainedGf(Wave wave, double absBearing, Range preciseMea) {
        MyRobot me = wave.getSnapshot().getOffset(-1);
        int direction = me.getDirection(wave.getSource());

        double absBearingWhenShot = Physics.absoluteBearing(wave.getSource(), me.getPoint());
        double offset = Utils.normalRelativeAngle(absBearing - absBearingWhenShot);

        if(offset * direction >= 0)
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

    private GuessFactorStats getStatsFromTree(KdTree<GuessFactorRange> t, double[] location, int K) {
        GuessFactorStats res = new GuessFactorStats(Double.POSITIVE_INFINITY);
        List<KdTree.Entry<GuessFactorRange>> entries = t.kNN(location, Math.min((int) R.sqrt(t.size()), K),
                1.0);

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
                new double[]{0.75, 0.25});
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
        log.preciseMea = preciseMea;
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
        final int WAVE_DIVISIONS = 121;

        G g = new G(graphics);

        if(_predicted != null) {
            for (Point data : _predicted) {
                g.drawPoint(data, 8.0, Color.RED);
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
                    * GuessFactorStats.BUCKET_COUNT * 0.8;
            st.setSmoother(new GaussianSmoothing(bandwidth * 0.6));

            double angle = 0;
            double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
            double maxDanger = 0;

            ArrayList<Shadow> shadows = shadowing.getShadows(wave);

            DangerPoint[] dangerPoints = new DangerPoint[WAVE_DIVISIONS];

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                angle += ratio;

                boolean usedShadow = false;
                for(Shadow shadow : shadows) {
                    if(shadow.isInside(angle)) {
                        usedShadow = true;
                        break;
                    }
                }

                if(usedShadow)
                    continue;

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
