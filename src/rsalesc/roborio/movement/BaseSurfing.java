package rsalesc.roborio.movement;

import robocode.Rules;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.movement.distancing.DistanceController;
import rsalesc.roborio.movement.distancing.FallbackDistanceController;
import rsalesc.roborio.movement.forces.DangerPoint;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.movement.predictor.WallSmoothing;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.*;
import rsalesc.roborio.utils.geo.*;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import static rsalesc.roborio.movement.predictor.WallSmoothing.WALL_STICK;

/**
 * Created by Roberto Sales on 27/08/17.
 * TODO: get correct stats for heat waves (future stats)
 */
public abstract class BaseSurfing extends Movement {
    private static KeyConfig config = KeyConfig.getInstance();

    protected double fallbackAngle;

    protected String hint;

    protected WaveMap<WaveSnap> waves;
    protected ShadowManager shadowing;
    protected HashSet<Wave> hasHit;

    protected BoxedInteger _bulletsFired;
    protected BoxedInteger _bulletsHit;

    protected long _wavesPassed = 0;
    protected long _shotsTaken = 0;

    protected boolean feltBack = false;

    private DistanceController fallbackController;

    public BaseSurfing(BackAsFrontRobot robot, String storageHint) {
        super(robot);
        waves = new WaveMap<>();
        shadowing = new ShadowManager();
        hasHit = new HashSet<>();
        fallbackController = new FallbackDistanceController();

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
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        super.onKeyPressed(e);
    }

    public WaveCollection getWaves() {
        return waves;
    }

    protected void checkImaginary() {
        Iterator<Wave> iterator = waves.iterator();

        while (iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if(wave instanceof EnemyFireWave) {
                EnemyFireWave fireWave = (EnemyFireWave) wave;
                if(fireWave.isImaginary() && getTime() != fireWave.getTime())
                    iterator.remove();
            }
        }
    }

    protected void checkHits() {
        checkImaginary();

        Iterator<Wave> iterator = waves.iterator();

        MyLog myLog = MyLog.getInstance();

        while (iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if (wave.hasPassedRobot(getRobot().getPoint(), getTime()) || hasHit.contains(wave)) {
                TargetingLog f = waves.getData(wave).getLog();
                AngularRange intersection = null;

                if(!hasHit.contains(wave)) {
                    f.hitPosition = getRobot().getPoint();
                    f.hitAngle = Physics.absoluteBearing(wave.getSource(), f.hitPosition);
                    f.hitDistance = wave.getSource().distance(f.hitPosition);

                    double absBearing = f.hitAngle;
                    intersection = R.preciseIntersection(myLog,
                            wave, getTime(), absBearing);
                } else if(f.hitPosition == null) {
                    f.hitPosition = wave.getSource().project(f.hitAngle, wave.getSource().distance(getRobot().getPoint()));
                    f.hitDistance = wave.getSource().distance(getRobot().getPoint());
                }


                double distance = f.hitPosition.distance(wave.getSource());

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

    public abstract void log(TargetingLog f, BreakType type);

    protected double getPreciseDanger(Wave wave, WaveSnap snap, AngularRange intersection, PredictedPoint pass) {
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

        int iBucket = stats.getBucket(gfRange.min);
        int jBucket = stats.getBucket(gfRange.max);

        for (int i = iBucket; i <= jBucket; i++) {
            double gf = stats.getGuessFactor(i);
            value += stats.getValueFromBucket(i);
        }

//        if(gfRange.getLength() > R.EPSILON) {
//            value /= stats.getGuessFactor(jBucket) - stats.getGuessFactor(iBucket) + 1e-9;
//            value *= gfRange.getLength();
//        }

        double shadowFactor = 1.0 - shadowing.getIntersectionFactor(wave, intersection);
        value *= shadowFactor;

        // test not averaging the values
//        value /= jBucket - iBucket + 1;

        return value;
    }

    protected void fallback(ComplexEnemyRobot enemy) {
        feltBack = true;
        MyRobot my = MyLog.getInstance().getLatest();

        double distance = my.getPoint().distance(enemy.getPoint());
        double absBearing = Physics.absoluteBearing(enemy.getPoint(), my.getPoint());
        double perp = fallbackController.getPerpendiculator(distance);

        AxisRectangle shrinkedField = getRobot().getBattleField().shrink(18, 18);

        double clockwiseAngle = WallSmoothing.naive(
                shrinkedField,
                my.getPoint(),
                Utils.normalAbsoluteAngle(absBearing + perp),
                +1
        );
        double counterAngle = WallSmoothing.naive(
                shrinkedField,
                my.getPoint(),
                Utils.normalAbsoluteAngle(absBearing - perp),
                -1
        );

        getRobot().setMaxVelocity(Rules.MAX_VELOCITY);

        if(Math.abs(Utils.normalRelativeAngle(clockwiseAngle - absBearing))
                < Math.abs(Utils.normalRelativeAngle(counterAngle - absBearing))) {
            setBackAsFront(clockwiseAngle);
            fallbackAngle = clockwiseAngle;
        } else {
            setBackAsFront(counterAngle);
            fallbackAngle = counterAngle;
        }
    }

    protected TargetingLog getTargetingLog(EnemyWave wave) {
        MyLog myLog = MyLog.getInstance();

        double bulletSpeed = wave.getVelocity();
        double bulletPower = Physics.bulletPower(bulletSpeed);

        // get enemys position at the moment of shooting and my info at the moment before
        MyRobot fireMe = wave.getSnapshot().getOffset(-1); // decision time
        MyRobot me = wave.getSnapshot().getOffset(-2); // decision time
        MyRobot pastMe = wave.getSnapshot().getOffset(-3);

        ComplexEnemyRobot enemy = wave.getEnemy();

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(
                getRobot().getBattleField(),
                fireMe.getPredictionPoint(),
                wave,
                fireMe.getDirection(wave.getSource())
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

    public void drawWaves(Graphics2D gr) {
        final int WAVE_DIVISIONS = 400;

        G g = new G(gr);
        Wave earliestWave = waves.earliestFireWave(MyLog.getInstance().getLatest());

        if(feltBack) {
            g.drawString(new Point(10, 580), "FELTBACK");
            g.drawRadial(getRobot().getPoint(), fallbackAngle, 0, WALL_STICK, Color.GREEN);
        }

        for(Wave cur : waves) {
            if(!(cur instanceof EnemyFireWave))
                continue;

            EnemyFireWave wave = (EnemyFireWave) cur;

            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()),
                    (wave.isImaginary() ? Color.PINK : ( wave == earliestWave ? Color.WHITE : Color.BLUE)));

            TargetingLog log = waves.getData(wave).getLog();
            GuessFactorStats st = waves.getData(wave).getStats();

            Point zeroPoint = wave.getSource().project(log.getZeroGf(), wave.getDistanceTraveled(getTime()));

            g.drawLine(wave.getSource(), zeroPoint, Color.GRAY);

            if(config.get('s')) {
                if (log.preciseIntersection != null) {
                    double distance = wave.getDistanceTraveled(getTime());
                    g.drawRadial(wave.getSource(), log.preciseIntersection.getStartingAngle(),
                            distance + 8, distance + 16, Color.RED);
                    g.drawRadial(wave.getSource(), log.preciseIntersection.getEndingAngle(),
                            distance + 8, distance + 16, Color.RED);
                }

                double angle = 0;
                double ratio = R.DOUBLE_PI / WAVE_DIVISIONS;
                double maxDanger = 0;

                ArrayList<Shadow> shadows = shadowing.getShadows(wave);

                ArrayList<DangerPoint> dangerPoints = new ArrayList<>();

                for (int i = 0; i < WAVE_DIVISIONS; i++) {
                    angle += ratio;
                    Point hitPoint = wave.getSource().project(angle, wave.getDistanceTraveled(getTime()));

                    boolean usedShadow = false;
                    for (Shadow shadow : shadows) {
                        if (shadow.isInside(angle)) {
                            usedShadow = true;
                            break;
                        }
                    }

                    double gf = log.getUnconstrainedGfFromAngle(angle);

                    if (!R.nearOrBetween(-1, gf, +1))
                        continue;

                    if (usedShadow) {
                        dangerPoints.add(new DangerPoint(hitPoint, -1));
                        continue;
                    }

                    double value = st.getValue(gf);
                    dangerPoints.add(new DangerPoint(hitPoint, value));
                    maxDanger = Math.max(maxDanger, value);
                }

                if (R.isNear(maxDanger, 0)) continue;

                Collections.sort(dangerPoints);

                int cnt = 0;
                for (DangerPoint dangerPoint : dangerPoints) {
                    Color dangerColor = dangerPoint.getDanger() > -0.01
                            ? G.getDiscreteSafeColor(1.0 * ++cnt / dangerPoints.size())
                            : Color.PINK;

                    Point base = wave.getSource().project(wave.getAngle(dangerPoint), wave.getDistanceTraveled(getTime()) - 18);
                    g.drawLine(base, dangerPoint, dangerColor);
                }
            }
        }
    }
}
