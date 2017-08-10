package roborio.gunning;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GunFireEvent;
import roborio.gunning.utils.PowerSelection;
import roborio.gunning.utils.TargetingLog;
import roborio.movement.predictor.MovementPredictor;
import roborio.myself.MyLog;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;
import roborio.utils.geo.Range;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.storage.NamedStorage;
import roborio.utils.waves.MyFireWave;
import roborio.utils.waves.MyWave;
import roborio.utils.waves.Wave;
import roborio.utils.waves.WaveMap;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 28/07/17.
 *
 * TODO: precise from next position (already doing this for me, do for enemy too)
 */
public class DCGuessFactorGun extends AutomaticGun {

    private WaveMap<TargetingLog> waves;
    private double         _wouldHit;
    private GunFireEvent lastFireEvent;
    private double absFireAngle;

    private ComplexEnemyRobot _lastEnemy;
    private long              _bulletsFired;

    private TargetingLog lastFiringLog;
    private DCGuessFactorTargeting targeting;

    @SuppressWarnings("unchecked")
    public DCGuessFactorGun(BackAsFrontRobot robot, boolean isVirtual, String storageHint) {
        super(robot, isVirtual);

        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, new DCGuessFactorTargeting(storageHint + "_targeting"));
        }

        if(!store.contains(storageHint + "-fired")) {
            store.add(storageHint + "-fired", 0L);
        }

        _bulletsFired = (Long) store.get(storageHint + "-fired");
        targeting = (DCGuessFactorTargeting) store.get(storageHint);

        waves = new WaveMap<>();
        _wouldHit = 0;
    }

    @Override
    public void doGunning() {
        if(lastFireEvent == null) return;

        MyLog log = MyLog.getInstance();
        Wave wave = new MyWave(log, lastFireEvent.getVelocity());

        waves.add(wave, lastFiringLog);
    }

    public void onScan(ScannedRobotEvent e) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);

//        Point nextPosition = getRobot().impreciseNextPosition();

        double bulletPower = PowerSelection.naive(MyLog.getInstance().getLatest(), enemy, 0.0);
        double bulletSpeed = Rules.getBulletSpeed(bulletPower);

        checkHits(enemy);

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
            enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(2),
                        getRobot().getPoint(), getTime(), bulletSpeed), enemy.getDirection());

        double mea = Physics.maxEscapeAngle(bulletSpeed);

        TargetingLog firingLog = new TargetingLog();
        firingLog.source = getRobot().getPoint();
        firingLog.direction = enemy.getDirection();
        firingLog.distance = enemy.getDistance();
        firingLog.lateralVelocity = enemy.getLateralVelocity();
        firingLog.advancingVelocity = enemy.getApproachingVelocity();
        firingLog.accel = (enemy.getVelocity() - pastEnemy.getVelocity())
                * Math.signum(enemy.getVelocity() + 1e-8);
        firingLog.bulletPower = bulletPower;
        firingLog.bulletsFired = _bulletsFired;
        firingLog.absBearing = enemy.getAbsoluteBearing();
        firingLog.positiveEscape = Math.abs(preciseMea.max / mea);
        firingLog.negativeEscape = Math.abs(preciseMea.min / mea);

        if(firingLog.accel < 0)
            firingLog.accelDirection = -firingLog.direction;
        else
            firingLog.accelDirection = firingLog.direction;

        // doideira
        //firingLog.direction = firingLog.accelDirection;

        firingLog.timeAccel = firingLog.accel > 0 ? 0 : 1;
        firingLog.timeDecel = firingLog.accel < 0 ? 0 : 1;
        firingLog.timeRevert = enemy.getDirection() * pastEnemy.getDirection() < 0 ? 0 : 1;
        firingLog.revertLast20 = firingLog.timeRevert ^ 1;

        Range coveredLast20 = new Range();

        for(int i = 1; i < Math.min(50, enemyLog.size() - 1); i++) {
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

            if(i <= 20) {
                double curBearing = Physics.absoluteBearing(getRobot().getPoint(), curEnemy.getPoint());
                double curOffset = curBearing - firingLog.absBearing;
                double curGf = curOffset * firingLog.direction / mea;
                coveredLast20.push(curGf);

                if(curEnemy.getDirection() * lastEnemy.getDirection() < 0)
                    firingLog.revertLast20++;
            }
        }

        firingLog.coveredLast20 = coveredLast20.maxAbsolute();

        absFireAngle = targeting.generateFiringAngle(firingLog);
        _lastEnemy = enemy;
        lastFiringLog = firingLog;
        if(bulletPower > 0)
            setFireTo(absFireAngle, bulletPower);
    }


    private void checkHits(ComplexEnemyRobot enemy) {
        _wouldHit = 0;

        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasTouchedRobot(enemy.getPoint(), getTime())) {
                TargetingLog missLog = waves.getData(wave);
                missLog.source = wave.getSource();
                missLog.hitPosition = enemy.getPoint();
                missLog.hitAngle = Physics.absoluteBearing(wave.getSource(), enemy.getPoint());
                missLog.hitDistance = wave.getSource().distance(enemy.getPoint());

                if(wave instanceof MyFireWave) {
                    MyFireWave fireWave = (MyFireWave) wave;
                    Point projection = fireWave.project(fireWave.getAngle(), getTime());
                    if(enemy.getHitBox().contains(projection)) {
                        _wouldHit = 1.0;
                    }
                }

                targeting.log(missLog, !(wave instanceof MyFireWave));

                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "Dynamic Clustering GuessFactorGun";
    }

    @Override
    public double wouldHit() {
        return _wouldHit;
    }

    @Override
    public void onFire(GunFireEvent e) {
        lastFireEvent = e;
        Wave wave = new MyFireWave(MyLog.getInstance(), e.getAngle(), e.getVelocity());
        waves.add(wave, lastFiringLog);
        _bulletsFired++;
    }

    @Override
    public void onPaint(Graphics2D gr) {
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
