package rsalesc.roborio.movement;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.energy.HeatLog;
import rsalesc.roborio.energy.HeatTracker;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.movement.distancing.DefaultDistanceController;
import rsalesc.roborio.movement.distancing.DistanceController;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.movement.predictor.WallSmoothing;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.KeyConfig;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class TrueSurfing extends BaseSurfing {
    private Boolean stopped;

    private AxisRectangle field;
    private MyLog myLog;
    private EnemyLog targetLog;

    private GuessFactorDodging dodging;

    private Point _lastImpact;
    private DistanceController controller;

    public TrueSurfing(BackAsFrontRobot robot, String storageHint) {
        super(robot, storageHint);

        hasHit = new HashSet<>();
        field = robot.getBattleField();
        myLog = MyLog.getInstance();

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
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;

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
        HeatLog heatLog = HeatTracker.getInstance().ensure(e.getName());

        if(heatLog.hasShot(getTime() - 1)) {
            double power = heatLog.getLastShotPower();
            double speed = Rules.getBulletSpeed(power);
            long shotTime = getTime() - 1;

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);
            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(shotTime), speed);

            TargetingLog log = getTargetingLog(wave);
            waves.add(wave, new WaveSnap(log,
                    dodging.getStats(log, getViewCondition())));

            onFire(targetLog.getLatest(), power);
        } else if(heatLog.ticksToCool() == 1) {
            double power = getPowerPredictor().predictEnemyPower(MyLog.getInstance().getLatest(),
                        targetLog.getLatest(), 0, 0);
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);

            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime()), speed).imaginary();

            TargetingLog log = getTargetingLog(wave);
            waves.add(wave, new WaveSnap(log,
                    dodging.getStats(log, getViewCondition())));
        } else if(heatLog.ticksToCool() == 0) {
            double power = getPowerPredictor().predictEnemyPower(MyLog.getInstance().getLatest(),
                    targetLog.getLatest(), 0, 0);
            double speed = Rules.getBulletSpeed(power);

            MySnapshot snap = myLog.takeSnapshot(R.WAVE_SNAPSHOT_DIAMETER);

            EnemyFireWave wave = new EnemyFireWave(snap, targetLog.atLeastAt(getTime()), speed).imaginary();

            TargetingLog log = getTargetingLog(wave);
            waves.add(wave, new WaveSnap(log,
                    dodging.getStats(log, getViewCondition())));
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

    public Knn.ParametrizedCondition getViewCondition() {
        return new LateFlatteningCondition(getEnemyConfidence(), getRobot().getRoundNum(), getEnemyAverageDistance());
    }

    public double getEnemyConfidence() {
        return 0.99999 * _bulletsHit.toDouble() / (_bulletsFired.toDouble() + 1e-9);
    }

    public double getEnemyAverageDistance() {
        if(targetLog == null || targetLog.getLatest() == null)
            return 0.0;
        return targetLog.getAverageDistance();
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
    public void doMovement(boolean shielding) {
        checkHits();
        feltBack = false;
        stopped = false;

        final MyRobot me = myLog.getLatest();
        Wave nextWave = waves.earliestFireWave(me);

        boolean hasData = dodging.hasData(getViewCondition());

        if(shielding)
            return;

        if(nextWave == null || !controller.shouldSurf(nextWave.getSource().distance(me.getPoint()))
                || !hasData) {
            if(targetLog != null && targetLog.getLatest() != null)
                fallback(targetLog.getLatest());
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

            stopped = true;

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
                .predictOnWaveImpact(field, initialPoint, nextWave, +1, perp, true, false);

        List<PredictedPoint> counterPoints = MovementPredictor
                .predictOnWaveImpact(field, initialPoint, nextWave, -1, perp, true, false);

        List<PredictedPoint> stopPoints = MovementPredictor
                .predictOnWaveImpact(field, initialPoint, nextWave, stopDirection, perp, true, true);

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

        boolean stopProtection = false;
        boolean diveProtection = false;

        for(int i = 0; i < 3; i++) {
            res[i].danger *= Physics.bulletPower(nextWave.getVelocity());
            res[i].danger /= impactTime;
            res[i].danger *=
                    Math.pow(2.45, distanceToSource / res[i].passPoint.distance(nextWave.getSource()) - 1);
            if(diveProtection) {
                if (i == 1 && stopProtection) {
                    res[i].danger = 1e14;
                } else if (i == 1 || (i == 0 && stopDirection == -1) || (i == 2 && stopDirection == 1)) {
                    res[i].danger *= 50;
                }
            }
        }

        return res;
    }

    public void log(TargetingLog f, BreakType type) {
        dodging.log(f, type);
    }

    @Override
    public void onPaint(Graphics2D graphics) {
        G g = new G(graphics);

        if(KeyConfig.getInstance().get('m')) {
            if (stopped != null && stopped) {
                g.drawString(new Point(10, 550), "STOPPED");
            }

            g.drawString(new Point(700, 20), Double.toString(getEnemyConfidence()));
            g.drawString(new Point(700, 30), Double.toString(getEnemyAverageDistance()));

            g.drawString(new Point(740, 10),
                    DCMovement.getFlatteningCondition().test(getViewCondition()) ? "FLATTENING" : "");

            if (MovementPredictor.lastEscape != null) {
                for (Point pt : MovementPredictor.lastEscape) {
                    g.drawPoint(pt, 3.0, Color.WHITE);
                }
            }

            if (_lastImpact != null) {
                g.drawPoint(_lastImpact, Physics.BOT_WIDTH * 2, Color.WHITE);
            }

            drawWaves(graphics);
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
