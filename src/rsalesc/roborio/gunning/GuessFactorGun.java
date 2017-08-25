package rsalesc.roborio.gunning;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyLog;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.gunning.utils.GunFireEvent;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.utils.*;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 28/07/17.
 *
 * TODO: support melee 1v1
 */
public class GuessFactorGun extends AutomaticGun {
    private String storageHint;

    private WaveMap<TargetingLog> waves;
    private double         _wouldHit;
    private double          _wouldHitPower;
    private long            _wouldHitTime = -1;
    private double absFireAngle;

    private ComplexEnemyRobot _lastEnemy;
    private BoxedInteger _bulletsFired;

    private TargetingLog            lastFiringLog;
    private TargetingLog            nextFiringLog;
    private TargetingLog            lastMissLog;
    private GuessFactorTargeting targeting;

    @SuppressWarnings("unchecked")
    public GuessFactorGun(BackAsFrontRobot robot) {
        super(robot, false);

        waves = new WaveMap<>();
        _wouldHit = 0;
        _wouldHitPower = 0;
    }

    public GuessFactorGun setName(String name) {
        this.storageHint = name;
        return this;
    }

    public GuessFactorGun setTargeting(GuessFactorTargeting targeting) {
        this.targeting = targeting;
        return this;
    }

    @Override
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            if(targeting.getIdentifier() == null)
                targeting.setIdentifier(storageHint + "-targeting");
            if(!targeting.isBuilt())
                targeting.build();
            store.add(storageHint, targeting);
        }

        if(!store.contains(storageHint + "-fired")) {
            store.add(storageHint + "-fired", new BoxedInteger());
        }

        targeting = (GuessFactorTargeting) store.get(storageHint);
        _bulletsFired = (BoxedInteger) store.get(storageHint + "-fired");
    }

    @Override
    public void doGunning() {
        if(lastFiringLog == null || getLastGunFireEvent() == null || getLastFireTime() == getTime()) return;

        MyLog log = MyLog.getInstance();
        double power = getManager().selectPower(log.getLatest(), _lastEnemy);
        double speed = Rules.getBulletSpeed(power);

        Wave wave = new MyWave(log, speed);

        lastFiringLog.aiming = false;
        waves.add(wave, lastFiringLog);
    }

    public void onScan(ScannedRobotEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;

        Checkpoint.getInstance().enter("scan_dc");

        lastFiringLog = nextFiringLog;
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);

        Point nextPosition = getRobot().getNextPosition();

        double bulletPower = getManager().selectPower(MyLog.getInstance().getLatest(), enemy);
        double bulletSpeed = Rules.getBulletSpeed(bulletPower);

        checkHits(enemy);

        TargetingLog log = new TargetingLog();

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
                enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(2),
                        nextPosition, getTime() + 1, bulletSpeed), enemy.getDirection());

        log.preciseMea = preciseMea;

        log.time = getTime();
        log.aiming = true;
        log.velocity = enemy.getVelocity();
        log.source = nextPosition;
        log.direction = enemy.getDirection();
        log.distance = enemy.getDistance();
        log.lateralVelocity = enemy.getLateralVelocity();
        log.advancingVelocity = enemy.getApproachingVelocity();
        log.accel = (enemy.getVelocity() - pastEnemy.getVelocity())
                * Math.signum(enemy.getVelocity() + 1e-8);
        log.bulletPower = bulletPower;
        log.bulletsFired = _bulletsFired.toLong();
        log.absBearing = Physics.absoluteBearing(nextPosition, enemy.getPoint());
        log.bafHeading = enemy.getHeading();

        if(enemy.getAhead() < 0)
            log.bafHeading = Utils.normalAbsoluteAngle(log.bafHeading + R.PI);

        log.relativeHeading = Math.abs(Utils.normalRelativeAngle(log.bafHeading -
                                Physics.absoluteBearing(nextPosition, enemy.getPoint())));

        log.positiveEscape = R.getWallEscape(getRobot().getBattleField(), enemy.getPoint(), log.bafHeading);
        log.negativeEscape = R.getWallEscape(getRobot().getBattleField(), enemy.getPoint(),
                Utils.normalAbsoluteAngle(log.bafHeading + R.PI));

        if(log.accel < 0)
            log.accelDirection = -log.direction;
        else
            log.accelDirection = log.direction;

        // doideira
        //log.direction = log.accelDirection;

        final int backInTime = 120;

        log.gunHeat = getRobot().getGunHeat();
        log.timeAccel = log.accel > 0 ? 0 : 1;
        log.timeDecel = log.accel < 0 ? 0 : 1;
        log.timeRevert = enemy.getDirection() * pastEnemy.getDirection() < 0 ? 0 : 1;
        log.revertLast20 = log.timeRevert ^ 1;
        log.run = enemy.getVelocity() != pastEnemy.getVelocity() ? 0 : backInTime;
        log.lastRun = backInTime;

        Range coveredLast20 = new Range();

        for(int i = 1; i < Math.min(backInTime, enemyLog.size() - 1); i++) {
            ComplexEnemyRobot curEnemy = enemyLog.atLeastAt(getTime() - i);
            ComplexEnemyRobot lastEnemy = enemyLog.atLeastAt(getTime() - i - 1);
            double prevAccel = (curEnemy.getVelocity() - lastEnemy.getVelocity())
                    * Math.signum(curEnemy.getVelocity() + 1e-8);
            if(log.timeAccel == i && prevAccel <= 0)
                log.timeAccel++;
            if(log.timeDecel == i && prevAccel >= 0)
                log.timeDecel++;
            if(curEnemy.getDirection() * lastEnemy.getDirection() >= 0 && log.timeRevert == i)
                log.timeRevert++;
            if(log.run == backInTime && curEnemy.getVelocity() != lastEnemy.getVelocity())
                log.run = i;
            if(log.run != backInTime && curEnemy.getVelocity() != lastEnemy.getVelocity())
                log.lastRun = i - log.run;

            if(i <= 20) {
                double curBearing = Physics.absoluteBearing(nextPosition, curEnemy.getPoint());
                double curOffset = curBearing - log.absBearing;
                coveredLast20.push(log.getGf(curOffset));

                if(curEnemy.getDirection() * lastEnemy.getDirection() < 0)
                    log.revertLast20++;
            }
        }

        log.displaceLast10 = enemyLog.atLeastAt(getTime() - 10).getPoint()
                .distance(enemy.getPoint());

        log.coveredLast20 = coveredLast20.maxAbsolute();
        log.lastMissGF = 0;
        if(lastMissLog != null) {
            log.lastMissGF = log.getGf(Utils.normalRelativeAngle(lastMissLog.hitAngle - lastMissLog.absBearing));
        }

        _lastEnemy = enemy;
        nextFiringLog = log;

        // generate firing angle
        if(getRobot().getGunHeat() > getRobot().getGunCoolingRate() * 2 + R.EPSILON || bulletPower == 0) {
            setTurnGunToRadians(enemy.getAbsoluteBearing());
            Checkpoint.getInstance().leave("scan_dc");
            return;
        }

        absFireAngle = targeting.generateFiringAngle(log);
        setFireTo(absFireAngle, bulletPower);
        Checkpoint.getInstance().leave("scan_dc");
    }


    private void checkHits(ComplexEnemyRobot enemy) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(enemy);
        _wouldHit = 0;
        _wouldHitPower = 0;

        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasPassedRobot(enemy.getPoint(), getTime())) {
                double absBearing = enemy.getAbsoluteBearing();
                double distance = enemy.getPoint().distance(wave.getSource());

                TargetingLog missLog = waves.getData(wave);
                missLog.source = wave.getSource(); // or use next position?
                missLog.hitPosition = enemy.getPoint();

                AngularRange intersection = R.preciseIntersection(enemyLog,
                        wave, getTime(), absBearing);

                if(intersection == null) {
                    double bearingFromWave = Physics.absoluteBearing(wave.getSource(), enemy.getPoint());
                    intersection = new AngularRange(
                            bearingFromWave,
                            -Physics.hitAngle(distance),
                            +Physics.hitAngle(distance)
                    );
                }

                missLog.preciseIntersection = intersection;

                missLog.hitAngle = intersection.getAngle(intersection.getCenter());
                missLog.hitDistance = distance;

                if(wave instanceof MyFireWave) {
                    MyFireWave fireWave = (MyFireWave) wave;
                    if(intersection.isAngleNearlyContained(fireWave.getAngle())) {
                        double diff = Utils.normalRelativeAngle(intersection.getOffset(fireWave.getAngle())
                                - intersection.getCenter());
                        double band = intersection.getLength() * 0.75;
                        _wouldHitTime = getTime();
                        _wouldHit = R.gaussKernel(diff / band);
                        _wouldHitPower = fireWave.getPower();
                        targeting.log(missLog, BreakType.BULLET_HIT);
                    } else {
                        targeting.log(missLog, BreakType.BULLET_BREAK);
                    }

                    lastMissLog = missLog;
                } else {
                    targeting.log(missLog, BreakType.VIRTUAL_BREAK);
                }

                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return storageHint;
    }

    @Override
    public double wouldHit() {
        return getTime() == _wouldHitTime ? _wouldHit : 0;
    }

    @Override
    public double wouldHitPower() {
        return getTime() == _wouldHitTime ? _wouldHitPower : 0;
    }

    @Override
    public void onFire(GunFireEvent e) {
        Wave wave = new MyFireWave(MyLog.getInstance(), e.getAngle(), e.getVelocity());
        waves.add(wave, lastFiringLog);
        _bulletsFired.increment();
    }

    @Override
    public void onPaint(Graphics2D gr) {
        targeting.onPaint(gr);

        G g = new G(gr);

        GuessFactorStats _lastStats = targeting.getLastStats();

        if(_lastStats == null || _lastEnemy == null) return;

        double maxValue = _lastStats.get(_lastStats.getBestBucket());

        g.drawLine(getRobot().getPoint(),
                getRobot().getPoint().project(absFireAngle, 100),
                Color.WHITE);

        if(targeting.getLastMissLog() != null) {
            TargetingLog missLog = targeting.getLastMissLog();
            g.drawLine(missLog.source,
                    missLog.source.project(missLog.hitAngle, missLog.hitDistance),
                    Color.YELLOW);
            g.drawPoint(missLog.source, 6, Color.YELLOW);
            g.drawPoint(missLog.hitPosition, 36, Color.YELLOW);

            g.drawString(new Point(20, 20),"Corrected GF: " + targeting.getLastFiringFactor());
        }

        if(lastFiringLog != null) {
            double angle1 = lastFiringLog.getPreciseMea().max * lastFiringLog.direction
                    + lastFiringLog.absBearing;
            double angle2 = lastFiringLog.getPreciseMea().min * lastFiringLog.direction
                    + lastFiringLog.absBearing;
            double distance = lastFiringLog.distance;

            g.drawPoint(lastFiringLog.source.project(angle1, distance), 3, Color.WHITE);
            g.drawPoint(lastFiringLog.source.project(angle2, distance), 3, Color.WHITE);
        }

        if(targeting.getLastFiringFactor() != null)
            g.drawString(new Point(20, 40),"Current GF: " + targeting.getLastFiringFactor());

        for(int i = 0; i < _lastStats.size(); i += 3) {
            double gf = _lastStats.getGuessFactor(i);
            double gfValue = _lastStats.get(i);

            Range lastMea = targeting.getLastEscapeAngle();
            double dangerPercent = Math.sqrt(gfValue / maxValue);
            double angle = _lastEnemy.getAbsoluteBearing() +
                    (gf > 0 ? gf * lastMea.max :
                            -gf * lastMea.min) * _lastEnemy.getDirection();

            g.drawCircle(getRobot().getPoint().project(angle, 100), 3.0, G.getDangerColor(dangerPercent));
        }
    }
}
