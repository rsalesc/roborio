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
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.Checkpoint;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.MyFireWave;
import rsalesc.roborio.utils.waves.MyWave;
import rsalesc.roborio.utils.waves.Wave;
import rsalesc.roborio.utils.waves.WaveMap;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 28/07/17.
 *
 * TODO: check bullet intersection
 * TODO: support melee 1v1
 * TODO: add enemy precise intersection on wave
 */
public class DCGuessFactorGun extends AutomaticGun {
    private static final double TICK_WAVE_SPEED = 15.0;

    private String storageHint;

    private WaveMap<TargetingLog> waves;
    private double         _wouldHit;
    private double          _wouldHitPower;
    private long            _wouldHitTime = -1;
    private double absFireAngle;

    private ComplexEnemyRobot _lastEnemy;
    private Long              _bulletsFired;

    private TargetingLog            lastFiringLog;
    private TargetingLog            nextFiringLog;
    private TargetingLog            lastMissLog;
    private DCGuessFactorTargeting targeting;

    @SuppressWarnings("unchecked")
    public DCGuessFactorGun(BackAsFrontRobot robot) {
        super(robot, false);

        waves = new WaveMap<>();
        _wouldHit = 0;
        _wouldHitPower = 0;
    }

    public DCGuessFactorGun setName(String name) {
        this.storageHint = name;
        return this;
    }

    public DCGuessFactorGun setTargeting(DCGuessFactorTargeting targeting) {
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
            store.add(storageHint + "-fired", 0L);
        }

        targeting = (DCGuessFactorTargeting) store.get(storageHint);
        _bulletsFired = (Long) store.get(storageHint + "-fired");
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

        TargetingLog firingLog = new TargetingLog();

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
                enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(2),
                        nextPosition, getTime() + 1, bulletSpeed), enemy.getDirection());

        firingLog.preciseMea = preciseMea;

        firingLog.aiming = true;
        firingLog.velocity = enemy.getVelocity();
        firingLog.source = nextPosition;
        firingLog.direction = enemy.getDirection();
        firingLog.distance = enemy.getDistance();
        firingLog.lateralVelocity = enemy.getLateralVelocity();
        firingLog.advancingVelocity = enemy.getApproachingVelocity();
        firingLog.accel = (enemy.getVelocity() - pastEnemy.getVelocity())
                * Math.signum(enemy.getVelocity() + 1e-8);
        firingLog.bulletPower = bulletPower;
        firingLog.bulletsFired = _bulletsFired;
        firingLog.absBearing = Physics.absoluteBearing(nextPosition, enemy.getPoint());
        firingLog.bafHeading = enemy.getHeading();

        if(enemy.getAhead() < 0)
            firingLog.bafHeading = Utils.normalAbsoluteAngle(firingLog.bafHeading + R.PI);

        firingLog.relativeHeading = Math.abs(Utils.normalRelativeAngle(firingLog.bafHeading -
                                Physics.absoluteBearing(nextPosition, enemy.getPoint())));

        firingLog.positiveEscape = R.getWallEscape(getRobot().getBattleField(), enemy.getPoint(), firingLog.bafHeading);
        firingLog.negativeEscape = R.getWallEscape(getRobot().getBattleField(), enemy.getPoint(),
                Utils.normalAbsoluteAngle(firingLog.bafHeading + R.PI));

        if(firingLog.accel < 0)
            firingLog.accelDirection = -firingLog.direction;
        else
            firingLog.accelDirection = firingLog.direction;

        // doideira
        //firingLog.direction = firingLog.accelDirection;

        final int backInTime = 120;

        firingLog.gunHeat = getRobot().getGunHeat();
        firingLog.timeAccel = firingLog.accel > 0 ? 0 : 1;
        firingLog.timeDecel = firingLog.accel < 0 ? 0 : 1;
        firingLog.timeRevert = enemy.getDirection() * pastEnemy.getDirection() < 0 ? 0 : 1;
        firingLog.revertLast20 = firingLog.timeRevert ^ 1;
        firingLog.run = enemy.getVelocity() != pastEnemy.getVelocity() ? 0 : backInTime;
        firingLog.lastRun = backInTime;

        Range coveredLast20 = new Range();

        for(int i = 1; i < Math.min(backInTime, enemyLog.size() - 1); i++) {
            ComplexEnemyRobot curEnemy = enemyLog.atLeastAt(getTime() - i);
            ComplexEnemyRobot lastEnemy = enemyLog.atLeastAt(getTime() - i - 1);
            double prevAccel = (curEnemy.getVelocity() - lastEnemy.getVelocity())
                    * Math.signum(curEnemy.getVelocity() + 1e-8);
            if(firingLog.timeAccel == i && prevAccel <= 0)
                firingLog.timeAccel++;
            if(firingLog.timeDecel == i && prevAccel >= 0)
                firingLog.timeDecel++;
            if(curEnemy.getDirection() * lastEnemy.getDirection() >= 0 && firingLog.timeRevert == i)
                firingLog.timeRevert++;
            if(firingLog.run == backInTime && curEnemy.getVelocity() != lastEnemy.getVelocity())
                firingLog.run = i;
            if(firingLog.run != backInTime && curEnemy.getVelocity() != lastEnemy.getVelocity())
                firingLog.lastRun = i - firingLog.run;

            if(i <= 20) {
                double curBearing = Physics.absoluteBearing(nextPosition, curEnemy.getPoint());
                double curOffset = curBearing - firingLog.absBearing;
                coveredLast20.push(firingLog.getGf(curOffset));

                if(curEnemy.getDirection() * lastEnemy.getDirection() < 0)
                    firingLog.revertLast20++;
            }
        }

        firingLog.displaceLast10 = enemyLog.atLeastAt(getTime() - 10).getPoint()
                .distance(enemy.getPoint());

        firingLog.coveredLast20 = coveredLast20.maxAbsolute();
        firingLog.lastMissGF = 0;
        if(lastMissLog != null) {
            firingLog.lastMissGF = firingLog.getGf(Utils.normalRelativeAngle(lastMissLog.hitAngle - lastMissLog.absBearing));
        }

        _lastEnemy = enemy;
        nextFiringLog = firingLog;

        // generate firing angle
        if(getRobot().getGunHeat() > getRobot().getGunCoolingRate() * 2 + R.EPSILON || bulletPower == 0) {
            setTurnGunToRadians(enemy.getAbsoluteBearing());
            Checkpoint.getInstance().leave("scan_dc");
            return;
        }

        absFireAngle = targeting.generateFiringAngle(firingLog);
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
                    }

                    lastMissLog = missLog;
                }

                targeting.log(missLog, !(wave instanceof MyFireWave));

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
        _bulletsFired++;
    }

    @Override
    public void onPaint(Graphics2D gr) {
        targeting.onPaint(gr);

        G g = new G(gr);

        GuessFactorStats _lastStats = targeting._lastStats;

        if(_lastStats == null || _lastEnemy == null) return;

        double maxValue = _lastStats.get(_lastStats.getBestBucket());

        g.drawLine(getRobot().getPoint(),
                getRobot().getPoint().project(absFireAngle, 100),
                Color.WHITE);

        if(targeting._lastMissLog != null) {
            TargetingLog missLog = targeting._lastMissLog;
            g.drawLine(missLog.source,
                    missLog.source.project(missLog.hitAngle, missLog.hitDistance),
                    Color.YELLOW);
            g.drawPoint(missLog.source, 6, Color.YELLOW);
            g.drawPoint(missLog.hitPosition, 36, Color.YELLOW);

            g.drawString(new Point(20, 20),"Corrected GF: " + targeting._lastFiringGf);
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

        if(targeting._lastGf != null)
            g.drawString(new Point(20, 40),"Current GF: " + targeting._lastGf);

        for(int i = 0; i < _lastStats.size(); i += 3) {
            double gf = _lastStats.getGuessFactor(i);
            double gfValue = _lastStats.get(i);

            Range lastMea = targeting._lastEscapeAngle;
            double dangerPercent = Math.sqrt(gfValue / maxValue);
            double angle = _lastEnemy.getAbsoluteBearing() +
                    (gf > 0 ? gf * lastMea.max :
                            -gf * lastMea.min) * _lastEnemy.getDirection();

            g.drawCircle(getRobot().getPoint().project(angle, 100), 3.0, G.getDangerColor(dangerPercent));
        }
    }
}
