package rsalesc.roborio.movement;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.movement.distancing.DefaultDistanceController;
import rsalesc.roborio.movement.distancing.DistanceController;
import rsalesc.roborio.movement.forces.DangerPoint;
import rsalesc.roborio.movement.forces.GotoSurfingCandidate;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.*;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 20/08/17.
 */
public class GotoSurfing extends BaseSurfing {
    private DistanceController controller;

    private GuessFactorDodging dodging;

    private AxisRectangle field;
    private MyLog myLog;
    private EnemyLog targetLog;
    private Double lastEnergy = null;

    private ArrayList<Wave> _surfedWaves;

    private ArrayList<Point> _predicted;

    private Point _gotoPoint;

    private int _lastAwayDirection = 1;
    private String hint;

    public GotoSurfing(BackAsFrontRobot robot, String storageHint) {
        super(robot, storageHint);
        field = robot.getBattleField();
        myLog = MyLog.getInstance();

        _surfedWaves = new ArrayList<Wave>();
        controller = new DefaultDistanceController();
    }

    public GotoSurfing setDodging(GuessFactorDodging dodging) {
        this.dodging = dodging;
        return this;
    }

    public GotoSurfing build() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(hint + "-dodging")) {
            if(!dodging.isBuilt())
                dodging.build();
            store.add(hint + "-dodging", dodging);
        }

        dodging = (GuessFactorDodging) store.get(hint + "-dodging");
        return this;
    }

    @Override
    public void onStatus(StatusEvent e) {
        dodging.tick(getTime(), getRobot().getRoundNum());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        shadowing.onBulletHitBullet(e);

        double heading = e.getHitBullet().getHeadingRadians();
        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave instanceof EnemyFireWave) {
                EnemyFireWave enemyWave = (EnemyFireWave) wave;
                if(enemyWave.wasFiredBy(e.getHitBullet(), getTime())) {
                    TargetingLog f = waves.getData(enemyWave).getLog();
                    f.hitAngle = heading;
                    hasHit.add(wave);
                    break;
                }
            }
        }
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
                    TargetingLog f = waves.getData(enemyWave).getLog();
                    f.hitPosition = hitPoint;
                    f.hitDistance = hitPoint.distance(enemyWave.getSource());
                    f.hitAngle = Physics.absoluteBearing(wave.getSource(), hitPoint);
                    hasHit.add(enemyWave);

                    _shotsTaken++;
                    _bulletsHit.increment();
                    break;
                }
            }
        }
    }

    @Override
    public void doShadowing(VirtualBullet[] bullets) {
        shadowing.push(bullets);
        ArrayList<EnemyFireWave> fireWaves = new ArrayList<>();
        for(Wave wave : waves) {
            if(wave instanceof EnemyFireWave)
                fireWaves.add((EnemyFireWave) wave);
        }

        shadowing.push(fireWaves.toArray(new EnemyFireWave[0]));
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

            TargetingLog log = getTargetingLog(wave);
            waves.add(wave, new WaveSnap(log, dodging.getStats(log, getViewCondition())));

            onFire(targetLog.getLatest(), power);
        }

        // flattening
        {
            double power = getPowerPredictor()
                    .predictEnemyPower(MyLog.getInstance().getLatest(), targetLog.getLatest(), 0, 0);
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyWave wave = new EnemyWave(snap, targetLog.atLeastAt(getTime()), speed);

            TargetingLog log = getTargetingLog(wave);
            waves.add(wave, new WaveSnap(log, null));
        }
    }

    public double getEnemyConfidence() {
        return 0.99999 * _bulletsHit.toDouble() / (_bulletsFired.toDouble() + 1e-9);
    }

    @Override
    public void doMovement() {
        checkHits();

        // stop surfing current wave on touch
        final MyRobot me = myLog.getLatest();
        Wave[] _nextWaves = waves.earliestWaves(2, me.getPoint() , getTime(), new WaveCollection.EnemyFireWaveCondition() {
            @Override
            public boolean test(Wave wave) {
                return super.test(wave) && !wave.hasTouchedRobot(me.getPoint(), me.getTime());
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
            logs[i] = waves.getData(nextWaves[i]).getLog();
            sts[i] = waves.getData(nextWaves[i]).getStats();
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

        MyRobot currentMe = MyLog.getInstance().getLatest();

        double[] mx = new double[sts.length];
        for(int i = 0; i < sts.length; i++) {
            mx[i] = 1.0;
            mx[i] = 1e-8;
            for(int j = 0; j < GuessFactorStats.BUCKET_COUNT; j++)
                mx[i] = Math.max(mx[i], sts[i].get(j));

            double distanceToSource = currentMe.getPoint().distance(nextWaves[i].getSource());
            double impactTime = Math.max(1, nextWaves[i].getVelocity()
                / (distanceToSource - nextWaves[i].getDistanceTraveled(getTime())));
            mx[i] = Physics.bulletPower(nextWaves[i].getVelocity()) / mx[i];
            mx[i] /= impactTime;
        }

        Range preciseMea = log.getPreciseMea();

        GotoSurfingCandidate[] candidates =
                getSurfingCandidates(myLog.getLatest().getPredictionPoint(), nextWave, stats, preciseMea, false);
        DangerPoint best = null;
        Point[] predicted = new Point[0];

        for(GotoSurfingCandidate c1 : candidates) {
            double value = c1.getDanger() * mx[0];
            if(best != null && best.getDanger() < value)
                break;

            if(nextWaves.length > 1) {
                Range preciseMea2 = logs[1].getPreciseMea();

                GotoSurfingCandidate[] futureCandidates =
                        getSurfingCandidates(c1.getPassPoint(), nextWaves[1], sts[1], preciseMea2, true);

                for (GotoSurfingCandidate c2 : futureCandidates) {
                    double nextValue = Math.max(c1.getDanger() * mx[0] + c2.getDanger() * mx[1], value);
                    if(best != null && best.getDanger() < nextValue)
                        break;

                    value = nextValue;

                    if (best == null || best.getDanger() > value) {
                        best = new DangerPoint(c1, value);
                        predicted = new Point[]{c1, c1.getPassPoint()};
                    }
                }
            } else {
                if (best == null || best.getDanger() > value) {
                    best = new DangerPoint(c1, value);
                    predicted = new Point[]{c1, c1.getPassPoint()};
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
            double offset = controller.getPerpendiculator(distance);

            AxisRectangle field = getRobot().getBattleField().shrink(18, 18);
            while(!field.strictlyContains(getRobot().getPoint().project(absBearing + offset * _lastAwayDirection, 160))) {
                offset += _lastAwayDirection * 0.05;
            }

            setBackAsFront(absBearing + offset * _lastAwayDirection);

            if(Math.abs(offset) > 4 * R.PI / 5)
                _lastAwayDirection *= -1;
        }
    }

    public Knn.ParametrizedCondition getViewCondition() {
        return new Knn.HitLeastCondition(getEnemyConfidence(), getRobot().getRoundNum());
    }

    public void log(TargetingLog f, BreakType type) {
        dodging.log(f, type);
    }

    private TargetingLog getTargetingLog(EnemyWave wave) {
        double bulletSpeed = wave.getVelocity();
        double bulletPower = Physics.bulletPower(bulletSpeed);

        // get enemys position at the moment of shooting and my info at the moment before
        MyRobot me = wave.getSnapshot().getOffset(-2); // decision time
        MyRobot pastMe = wave.getSnapshot().getOffset(-3);

        ComplexEnemyRobot enemy = wave.getEnemy();

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(
                field,
                me.getPredictionPoint(),
                wave,
                me.getDirection(wave.getSource())
        );

        TargetingLog log = new TargetingLog();
        log.preciseMea = preciseMea;
        log.time = wave.getTime() - 1; // is this right?
        log.velocity = me.getVelocity();
        log.source = wave.getSource();
        log.direction = me.getDirection(wave.getSource()); // improve this?
        log.distance = wave.getEnemy().getDistance();
        log.absBearing = wave.getAngle(me.getPoint());
        log.lateralVelocity = me.getLateralVelocity(wave.getSource());
        log.advancingVelocity = me.getApproachingVelocity(wave.getSource());
        log.bulletPower = bulletPower;
        log.bulletsFired = _bulletsFired.toLong();
        log.distance = me.getPoint().distance(wave.getSource());
        log.accel = (me.getVelocity() - pastMe.getVelocity())
                * Math.signum(me.getVelocity() + 1e-8);

        log.bafHeading = me.getHeading();

        if(me.getAhead() < 0)
            log.bafHeading = Utils.normalAbsoluteAngle(log.bafHeading + R.PI);

        log.relativeHeading = Math.abs(Utils.normalRelativeAngle(log.bafHeading -
                Physics.absoluteBearing(wave.getSource(), me.getPoint())));

        log.positiveEscape = R.getWallEscape(getRobot().getBattleField(), me.getPoint(), log.bafHeading);
        log.negativeEscape = R.getWallEscape(getRobot().getBattleField(), me.getPoint(),
                Utils.normalAbsoluteAngle(log.bafHeading + R.PI));

        if(log.accel < 0)
            log.accelDirection = -log.direction;
        else
            log.accelDirection = log.direction;

        final int backInTime = 120;

        log.timeAccel = log.accel > 0 ? 0 : 1;
        log.timeDecel = log.accel < 0 ? 0 : 1;
        log.timeRevert = me.getDirection(enemy.getPoint()) * pastMe.getDirection(enemy.getPoint()) < 0 ? 0 : 1;
        log.revertLast20 = log.timeRevert ^ 1;
        log.run = me.getVelocity() != pastMe.getVelocity() ? 0 : backInTime;
        log.lastRun = backInTime;

        Range coveredLast20 = new Range();

        for(int i = 1; i < backInTime; i++) {
            MyRobot curMe = myLog.atLeastAt(wave.getTime() - i - 1); // change to snapshot
            MyRobot lastMe = myLog.atLeastAt(wave.getTime() - i - 2);

            if(curMe == lastMe)
                break;

            double prevAccel = (curMe.getVelocity() - lastMe.getVelocity())
                    * Math.signum(curMe.getVelocity() + 1e-8);
            if(log.timeAccel == i && prevAccel <= 0)
                log.timeAccel++;
            if(log.timeDecel == i && prevAccel >= 0)
                log.timeDecel++;
            if(curMe.getDirection(wave.getSource()) * lastMe.getDirection(wave.getSource()) >= 0 && log.timeRevert == i)
                log.timeRevert++;
            if(log.run == backInTime && curMe.getVelocity() != lastMe.getVelocity())
                log.run = i;
            if(log.run != backInTime && curMe.getVelocity() != lastMe.getVelocity())
                log.lastRun = i - log.run;

            if(i <= 20) {
                double curBearing = Physics.absoluteBearing(wave.getSource(), curMe.getPoint());
                double curOffset = curBearing - log.absBearing;
                coveredLast20.push(log.getGf(curOffset));

                if(curMe.getDirection(wave.getSource()) * lastMe.getDirection(wave.getSource()) < 0)
                    log.revertLast20++;
            }
        }

        log.displaceLast10 = myLog.atLeastAt(wave.getTime() - 11).getPoint()
                .distance(me.getPoint());

        log.displaceLast20 = myLog.atLeastAt(wave.getTime() - 21).getPoint()
                .distance(me.getPoint());

        log.displaceLast40 = myLog.atLeastAt(wave.getTime() - 41).getPoint()
                .distance(me.getPoint());

        log.displaceLast80 = myLog.atLeastAt(wave.getTime() - 81).getPoint()
                .distance(me.getPoint());

        log.displaceLast160 = myLog.atLeastAt(wave.getTime() - 161).getPoint()
                .distance(me.getPoint());

        log.coveredLast20 = coveredLast20.maxAbsolute();

        return log;
    }

    private GotoSurfingCandidate[] getSurfingCandidates(PredictedPoint initialPoint,
                                                        EnemyFireWave wave, GuessFactorStats stats, Range preciseMea,
                                                        boolean fast) {

        WaveSnap snap = waves.getData(wave);
        MyRobot me = wave.getSnapshot().getOffset(-1);

        double distance = initialPoint.distance(wave.getSource());
        double perp = controller.getPerpendiculator(distance);

        List<PredictedPoint> genPoints = MovementPredictor.generateOnWaveImpact(
                field, initialPoint,
                wave, +1, perp, false);

        List<PredictedPoint> otherPoints = MovementPredictor.generateOnWaveImpact(
                field, initialPoint,
                wave, -1, perp, false);

        Collections.reverse(genPoints);
        genPoints.addAll(otherPoints);


        if(genPoints.size() == 0)
            throw new IllegalStateException();

        ArrayList<GotoSurfingCandidate> dangers = new ArrayList<>();
        PredictedPoint first = genPoints.get(0);
        PredictedPoint back = genPoints.get(genPoints.size() - 1);
        PredictedPoint last = null;

        final int CANDIDATE_STEP = 20;

        for(PredictedPoint predicted : genPoints) {
            if(last == null || last == first || last == back || last.distance(predicted) > CANDIDATE_STEP) {
                last = predicted;

                // TODO: use gauss smoothing over here
                double value = 0;

                List<PredictedPoint> points = MovementPredictor.predictOnWaveImpact(
                        initialPoint,
                        wave,
                        predicted,
                        !fast // true maybe?
                );

                if(!fast) {
                    PredictedPoint passPoint = R.getLast(points);
                    AngularRange intersection =
                            R.preciseIntersection(wave, points);

                    value = getPreciseDanger(wave, snap, intersection, passPoint);
                    value *= Math.pow(2.45, distance / passPoint.distance(wave.getSource()) - 1);

                    dangers.add(new GotoSurfingCandidate(predicted, value, passPoint));
                } else {
                    PredictedPoint impactPoint = R.getLast(points);
                    value = getPreciseDanger(wave, snap, null, impactPoint);
                    value *= Math.pow(2.45, distance / impactPoint.distance(wave.getSource()) - 1);

                    dangers.add(new GotoSurfingCandidate(predicted, value, impactPoint));
                }
            }
        }

        Collections.sort(dangers);
        return dangers.toArray(new GotoSurfingCandidate[0]);
    }

    private void goTo(Point dest) {
        if(!_surfedWaves.isEmpty() && _gotoPoint != null) {
            getRobot().moveWithBackAsFront(dest, getRobot().getPoint().distance(dest));
        }
    }

    private double getSteepness(Point dest, Point enemy) {
        Point my = getRobot().getPoint();
        double angleEnemy = Physics.absoluteBearing(my, enemy);
        double angleDest = Physics.absoluteBearing(my, dest);
        return Math.abs(Utils.normalRelativeAngle(angleDest - angleEnemy));
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        final int WAVE_DIVISIONS = 400;

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

        drawWaves(graphics);
    }

    public void printLog() {
        System.out.println("Waves passed: " + _wavesPassed);
        System.out.println("Shots taken: " + _shotsTaken);
    }
}
