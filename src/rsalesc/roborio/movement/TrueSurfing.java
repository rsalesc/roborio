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
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.movement.predictor.WallSmoothing;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.BoxedInteger;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.*;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Roberto Sales on 21/08/17.
 * TODO: surf shadows
 * TODO: check why I am hiting the wall constantly
 * TODO: do not surf bullet hit bullet wave
 */
public class TrueSurfing extends Movement {
    private static final double IDEAL_DISTANCE = 500;

    private AxisRectangle field;
    private MyLog myLog;
    private EnemyLog targetLog;
    private Double lastEnergy = null;

    private GuessFactorDodging dodging;
    private WaveMap<WaveSnap> waves;
    private HashSet<Wave> hasHit;

    private BoxedInteger _bulletsFired;
    private BoxedInteger _bulletsHit;

    private Point _lastImpact;

    private int _shotsTaken = 0;
    private int _wavesPassed = 0;

    private String hint;
    private int _lastAwayDirection = 1;

    private ShadowManager shadowing;
    private DistanceController controller;

    public TrueSurfing(BackAsFrontRobot robot, String storageHint) {
        super(robot);

        shadowing = new ShadowManager();
        hasHit = new HashSet<>();
        waves = new WaveMap<>();
        field = robot.getBattleField();
        myLog = MyLog.getInstance();
        this.hint = storageHint;

        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint + "-fired")) {
            store.add(storageHint + "-fired", new BoxedInteger());
        }

        if(!store.contains(storageHint + "-hit")) {
            store.add(storageHint + "-hit", new BoxedInteger());
        }

        _bulletsFired = (BoxedInteger) store.get(storageHint + "-fired");
        _bulletsHit = (BoxedInteger) store.get(storageHint + "-hit");
        controller = new DefaultDistanceController();
    }

    public TrueSurfing setDodging(GuessFactorDodging dodging) {
        this.dodging = dodging;
        return this;
    }

    public TrueSurfing build() {
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
            waves.add(wave, new WaveSnap(log,
                    dodging.getStats(log, getEnemyConfidence(), getRobot().getRoundNum())));

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

    private void fallback() {
        if(targetLog != null) {
            MyRobot my = MyLog.getInstance().getLatest();
            ComplexEnemyRobot enemy = targetLog.getLatest();

            double distance = my.getPoint().distance(enemy.getPoint());
            double absBearing = Physics.absoluteBearing(enemy.getPoint(), my.getPoint());
            double offset = controller.getPerpendiculator(distance);

            AxisRectangle field = getRobot().getBattleField().shrink(18, 18);
            while(!field.strictlyContains(getRobot().getPoint().project(absBearing + offset * _lastAwayDirection, 215))) {
                offset += _lastAwayDirection * 0.05;
            }

            setBackAsFront(absBearing + offset * _lastAwayDirection);

            if(Math.abs(offset) > 4 * R.PI / 5)
                _lastAwayDirection *= -1;
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
    public void doMovement() {
        checkHits();

        final MyRobot me = myLog.getLatest();
        Wave nextWave = waves.earliestFireWave(me);

        if(nextWave == null || !controller.shouldSurf(nextWave.getSource().distance(me.getPoint()))) {
            fallback();
            return;
        }

        double stopDanger = 0;
        double clockwiseDanger = 0;
        double counterDanger = 0;

        AxisRectangle shrinkedField = field.shrinkX(18).shrinkY(18);
        Wave secondWave = waves.earliestFireWave(me.getPoint(), (long) (nextWave.getBreakTime(me.getPoint()) + 1));
        SurfingCandidate[] firstCandidates = getSurfingCandidates(me.getPredictionPoint(), nextWave);

        for(int i = 0; i < 3; i++) {
            double currentDanger = firstCandidates[i].danger;
            SurfingCandidate[] secondCandidates = getSurfingCandidates(firstCandidates[i].passPoint, secondWave);
//            SurfingCandidate[] secondCandidates = null;
            if(secondCandidates != null) {
                double bestCompoundDanger = Double.POSITIVE_INFINITY;
                for(int j = 0; j < 3; j++) {
                    bestCompoundDanger = Math.min(bestCompoundDanger, currentDanger + secondCandidates[j].danger);
                }
                currentDanger = bestCompoundDanger;
            }

            if(i == 0) clockwiseDanger = currentDanger;
            else if(i == 1) stopDanger = currentDanger;
            else counterDanger = currentDanger;
        }

        double distance = nextWave.getSource().distance(me.getPoint());
        double perp = controller.getPerpendiculator(distance);
        if (stopDanger < counterDanger && stopDanger < clockwiseDanger) {
            int stopDirection = me.getDirection(nextWave.getSource());
            if(stopDirection == 0) stopDirection = 1;

            getRobot().setMaxVelocity(0);
            double angle = Utils.normalAbsoluteAngle(WallSmoothing.naive(shrinkedField, me.getPoint(),
                    Physics.absoluteBearing(nextWave.getSource(), me.getPoint())
                            + perp * stopDirection, stopDirection));
            getRobot().setBackAsFront(angle);
            _lastImpact = firstCandidates[1].passPoint;
        } else if (clockwiseDanger < counterDanger) {
            double angle = Utils.normalAbsoluteAngle(WallSmoothing.naive(shrinkedField, me.getPoint(),
                    Physics.absoluteBearing(nextWave.getSource(), me.getPoint())
                            + perp, +1));
            getRobot().setMaxVelocity(Rules.MAX_VELOCITY);
            getRobot().setBackAsFront(angle);
            _lastImpact = firstCandidates[0].passPoint;
        } else {
            double angle = Utils.normalAbsoluteAngle(WallSmoothing.naive(shrinkedField, me.getPoint(),
                    Physics.absoluteBearing(nextWave.getSource(), me.getPoint())
                            - perp, -1));
            getRobot().setMaxVelocity(Rules.MAX_VELOCITY);
            getRobot().setBackAsFront(angle);
            _lastImpact = firstCandidates[2].passPoint;
        }
    }

    private SurfingCandidate[] getSurfingCandidates(PredictedPoint initialPoint, Wave nextWave) {
        if(nextWave == null)
            return null;

        WaveSnap snap = waves.getData(nextWave);
        double distance = nextWave.getSource().distance(initialPoint);
        double perp = controller.getPerpendiculator(distance);

        int stopDirection = initialPoint.getDirection(nextWave.getSource());
        if(stopDirection == 0) stopDirection = 1;

        List<PredictedPoint> clockwisePoints = MovementPredictor
                .predictOnWaveImpact(field, initialPoint, nextWave, +1, perp, true);

        List<PredictedPoint> counterPoints = MovementPredictor
                .predictOnWaveImpact(field, initialPoint, nextWave, -1, perp, true);

        List<PredictedPoint> stopPoints = MovementPredictor
                .predictOnWaveImpact(field, initialPoint, nextWave, stopDirection, perp, true);

        PredictedPoint clockwisePass = R.getLast(clockwisePoints);
        PredictedPoint counterPass = R.getLast(counterPoints);
        PredictedPoint stopPass = R.getLast(stopPoints);

        AngularRange clockwiseIntersection =
                R.preciseIntersection(nextWave, clockwisePoints);
        AngularRange counterIntersection =
                R.preciseIntersection(nextWave, counterPoints);
        AngularRange stopIntersection =
                R.preciseIntersection(nextWave, stopPoints);

        double clockwiseDanger = getPreciseDanger(nextWave, snap, clockwiseIntersection, clockwisePass);
        double counterDanger = getPreciseDanger(nextWave, snap, counterIntersection, counterPass);
        double stopDanger = getPreciseDanger(nextWave, snap, stopIntersection, stopPass);

        SurfingCandidate[] res = new SurfingCandidate[]{
                new SurfingCandidate(clockwiseDanger, clockwisePass),
                new SurfingCandidate(stopDanger, stopPass),
                new SurfingCandidate(counterDanger, counterPass)
        };

        double distanceToSource = initialPoint.distance(nextWave.getSource());
        double impactTime = Math.max(nextWave.getVelocity() /
                (distanceToSource - nextWave.getDistanceTraveled(initialPoint.getTime())), 1);

        for(int i = 0; i < 3; i++) {
            res[i].danger *= Physics.bulletPower(nextWave.getVelocity());
            res[i].danger /= impactTime;
            res[i].danger *=
                    Math.pow(2.45, distanceToSource / res[i].passPoint.distance(nextWave.getSource()) - 1);
        }

        return res;
    }

    private double getPreciseDanger(Wave wave, WaveSnap snap, AngularRange intersection, PredictedPoint pass) {
        TargetingLog log = snap.getLog();
        GuessFactorStats stats = snap.getStats();

        if(intersection == null) {
            double distance = wave.getSource().distance(pass);
            double passBearing = Physics.absoluteBearing(wave.getSource(), pass);
            double width = Physics.hitAngle(distance);
            intersection = new AngularRange(passBearing, -width, width);
        }

        double gfLow = log.getGfFromAngle(intersection.getStartingAngle());
        double gfHigh = log.getGfFromAngle(intersection.getEndingAngle());

        Range gfRange = new Range();
        gfRange.push(gfLow);
        gfRange.push(gfHigh);

        double value = 0;

        int iBucket = stats.getBucket(gfRange.min)-1;
        int jBucket = stats.getBucket(gfRange.max)+1;
        if(jBucket >= GuessFactorStats.BUCKET_COUNT)
            jBucket = GuessFactorStats.BUCKET_COUNT - 1;
        if(iBucket < 0)
            iBucket = 0;

        for (int i = iBucket; i <= jBucket; i++) {
            double gf = stats.getGuessFactor(i);
            double angle = Utils.normalAbsoluteAngle(log.getOffset(gf) + log.absBearing);

            value += stats.getValueFromBucket(i);
        }

        if(gfRange.getLength() > R.EPSILON) {
            value /= stats.getGuessFactor(jBucket) - stats.getGuessFactor(iBucket) + 1e-9;
            value *= gfRange.getLength();
        }

//        value *= 1.0 - shadowing.getIntersectionFactor(wave, intersection);

        // test not averaging the values
//        value /= jBucket - iBucket + 1;

        return value;
    }

    private void checkHits() {
        Iterator<Wave> iterator = waves.iterator();

        while (iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if (wave.hasPassedRobot(getRobot().getPoint(), getTime())) {
                TargetingLog f = waves.getData(wave).getLog();

                // TODO: use precise intersection to compute hit point
                if(f.hitPosition == null) {
                    f.hitPosition = getRobot().getPoint();
                    f.hitAngle = Physics.absoluteBearing(wave.getSource(), f.hitPosition);
                    f.hitDistance = wave.getSource().distance(f.hitPosition);
                }

                double absBearing = f.hitAngle;
                double distance = f.hitPosition.distance(wave.getSource());

                AngularRange intersection = R.preciseIntersection(myLog,
                        wave, getTime(), absBearing);

                if(intersection == null) {
                    double bearingFromWave = Physics.absoluteBearing(wave.getSource(), f.hitPosition);
                    intersection = new AngularRange(
                            bearingFromWave,
                            -Physics.hitAngle(distance),
                            +Physics.hitAngle(distance)
                    );
                }

                f.preciseIntersection = intersection;

                if(wave instanceof EnemyFireWave) {
                    if(hasHit.contains(wave)) {
                        log(f, BreakType.BULLET_HIT);
                        hasHit.remove(wave);
                    } else {
                        log(f, BreakType.BULLET_BREAK);
                    }
                    _bulletsFired.increment();
                }
                else {
                    log(f, BreakType.VIRTUAL_BREAK);
                }

                iterator.remove();
                _wavesPassed++;
            }
        }
    }

    private void log(TargetingLog f, BreakType type) {
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

        double halfWidth = Physics.hitAngle(wave.getSource().distance(me.getPoint())) / 2;
        preciseMea.push(preciseMea.max + halfWidth);
        preciseMea.push(preciseMea.min - halfWidth);

        TargetingLog log = new TargetingLog();
        log.preciseMea = preciseMea;
        log.time = wave.getTime(); // is this right?
        log.velocity = me.getVelocity();
        log.source = wave.getSource();
        log.direction = me.getDirection(wave.getSource()); // improve this?
        log.distance = wave.getSource().distance(me.getPoint());
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

    @Override
    public void onPaint(Graphics2D graphics) {
        final int WAVE_DIVISIONS = 400;

        G g = new G(graphics);

        if(_lastImpact != null) {
            g.drawPoint(_lastImpact, Physics.BOT_WIDTH * 2, Color.WHITE);
        }

        Wave earliestWave = waves.earliestFireWave(myLog.getLatest());

        for(Wave cur : waves) {
            if(!(cur instanceof EnemyFireWave))
                continue;

            EnemyFireWave wave = (EnemyFireWave) cur;

            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()),
                    wave == earliestWave ? Color.WHITE : Color.BLUE);

            TargetingLog log = waves.getData(wave).getLog();
            GuessFactorStats st = waves.getData(wave).getStats();

            double angle = 0;
            double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
            double maxDanger = 0;

            ArrayList<Shadow> shadows = shadowing.getShadows(wave);

            ArrayList<DangerPoint> dangerPoints = new ArrayList<>();

            for(int i = 0; i < WAVE_DIVISIONS; i++) {
                angle += ratio;
                Point hitPoint = wave.getSource().project(angle, wave.getDistanceTraveled(getTime()));

                boolean usedShadow = false;
                for(Shadow shadow : shadows) {
                    if(shadow.isInside(angle)) {
                        usedShadow = true;
                        break;
                    }
                }

                double gf = log.getUnconstrainedGfFromAngle(angle);

                if(!R.nearOrBetween(-1, gf, +1))
                    continue;

                if(usedShadow) {
                    dangerPoints.add(new DangerPoint(hitPoint, -1));
                    continue;
                }

                double value = st.getValue(gf);
                dangerPoints.add(new DangerPoint(hitPoint, value));
                maxDanger = Math.max(maxDanger, value);
            }

            if(R.isNear(maxDanger, 0)) continue;

            Collections.sort(dangerPoints);

            int cnt = 0;
            for(DangerPoint dangerPoint : dangerPoints) {
                Color dangerColor = dangerPoint.getDanger() > -0.01
                        ? G.getDiscreteSafeColor(1.0 * ++cnt / dangerPoints.size())
                        : Color.PINK;

                Point base = wave.getSource().project(wave.getAngle(dangerPoint), wave.getDistanceTraveled(getTime()) - 18);
                g.drawLine(base, dangerPoint, dangerColor);
            }
        }
    }

    private class SurfingCandidate {
        public double danger;
        public final PredictedPoint passPoint;

        public SurfingCandidate(double danger, PredictedPoint passPoint) {
            this.danger = danger;
            this.passPoint = passPoint;
        }
    }
}
