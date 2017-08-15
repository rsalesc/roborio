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
 */
public class DCGuessFactorGun extends AutomaticGun {
    private String storageHint;

    private WaveMap<TargetingLog> waves;
    private double         _wouldHit;
    private double          _wouldHitPower;
    private GunFireEvent lastFireEvent;
    private double absFireAngle;

    private ComplexEnemyRobot _lastEnemy;
    private Long              _bulletsFired;

    private TargetingLog            lastFiringLog;
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
        if(lastFireEvent == null) return;

        MyLog log = MyLog.getInstance();
        Wave wave = new MyWave(log, lastFireEvent.getVelocity());

        lastFiringLog.aiming = false;
        waves.add(wave, lastFiringLog);
    }

    public void onScan(ScannedRobotEvent e) {
        if(R.isNear(getRobot().getEnergy(), 0))
            return;

        Checkpoint.getInstance().enter("scan_dc");

        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);

        Point nextPosition = getRobot().impreciseNextPosition();

        double bulletPower = getManager().selectPower(MyLog.getInstance().getLatest(),
                enemy, 0, 0);
        double bulletSpeed = Rules.getBulletSpeed(bulletPower);

        checkHits(enemy);

        if(getRobot().getGunHeat() > getRobot().getGunCoolingRate() + R.EPSILON || bulletPower == 0) {
            setTurnGunToRadians(enemy.getAbsoluteBearing());
            Checkpoint.getInstance().leave("scan_dc");
            return;
        }

        Range preciseMea = MovementPredictor.getBetterMaximumEscapeAngle(getRobot().getBattleField(),
            enemy.getPredictionPoint(), new Wave(MyLog.getInstance().takeSnapshot(2),
                        nextPosition, getTime() + 1, bulletSpeed), enemy.getDirection());

        TargetingLog firingLog = new TargetingLog();
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
        firingLog.preciseMea = preciseMea;

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

        firingLog.gunHeat = getRobot().getGunHeat();
        firingLog.timeAccel = firingLog.accel > 0 ? 0 : 1;
        firingLog.timeDecel = firingLog.accel < 0 ? 0 : 1;
        firingLog.timeRevert = enemy.getDirection() * pastEnemy.getDirection() < 0 ? 0 : 1;
        firingLog.revertLast20 = firingLog.timeRevert ^ 1;
        firingLog.run = enemy.getVelocity() != pastEnemy.getVelocity() ? 0 : 50;
        firingLog.lastRun = 50;

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
            if(firingLog.run == 50 && curEnemy.getVelocity() != lastEnemy.getVelocity())
                firingLog.run = i;
            if(firingLog.run != 50 && curEnemy.getVelocity() != lastEnemy.getVelocity())
                firingLog.lastRun = i - firingLog.run;

            if(i <= 20) {
                double curBearing = Physics.absoluteBearing(nextPosition, curEnemy.getPoint());
                double curOffset = curBearing - firingLog.absBearing;
                coveredLast20.push(firingLog.getGf(curOffset));

                if(curEnemy.getDirection() * lastEnemy.getDirection() < 0)
                    firingLog.revertLast20++;
            }
        }

        firingLog.coveredLast20 = coveredLast20.maxAbsolute();
        firingLog.lastMissGF = 0;
        if(lastMissLog != null) {
            firingLog.lastMissGF = firingLog.getGf(Utils.normalRelativeAngle(lastMissLog.hitAngle - lastMissLog.absBearing));
        }

        absFireAngle = targeting.generateFiringAngle(firingLog);
        _lastEnemy = enemy;
        lastFiringLog = firingLog;
        setFireTo(absFireAngle, bulletPower);
        Checkpoint.getInstance().leave("scan_dc");
    }


    private void checkHits(ComplexEnemyRobot enemy) {
        _wouldHit = 0;
        _wouldHitPower = 0;

        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasTouchedRobot(enemy.getPoint(), getTime())) {
                TargetingLog missLog = waves.getData(wave);
                missLog.source = wave.getSource(); // or use next position?
                missLog.hitPosition = enemy.getPoint();
                missLog.hitAngle = Physics.absoluteBearing(wave.getSource(), enemy.getPoint());
                missLog.hitDistance = wave.getSource().distance(enemy.getPoint());

                if(wave instanceof MyFireWave) {
                    MyFireWave fireWave = (MyFireWave) wave;
                    Point projection = fireWave.project(fireWave.getAngle(), getTime());
                    if(enemy.getHitBox().contains(projection)) {
                        _wouldHit = 1.0;
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
        return _wouldHit;
    }

    @Override
    public double wouldHitPower() {
        return _wouldHitPower;
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
