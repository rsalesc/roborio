package rsalesc.roborio.movement;

import robocode.util.Utils;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.movement.forces.DangerPoint;
import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.BoxedInteger;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 27/08/17.
 * TODO: mark bullet hitted waves as safe
 */
public abstract class BaseSurfing extends Movement {
    protected String hint;

    protected WaveMap<WaveSnap> waves;
    protected ShadowManager shadowing;
    protected HashSet<Wave> hasHit;

    protected BoxedInteger _bulletsFired;
    protected BoxedInteger _bulletsHit;

    protected long _wavesPassed = 0;
    protected long _shotsTaken = 0;

    public BaseSurfing(BackAsFrontRobot robot, String storageHint) {
        super(robot);
        waves = new WaveMap<>();
        shadowing = new ShadowManager();
        hasHit = new HashSet<>();

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

    protected void checkHits() {
        Iterator<Wave> iterator = waves.iterator();

        MyLog myLog = MyLog.getInstance();

        while (iterator.hasNext()) {
            EnemyWave wave = (EnemyWave) iterator.next();
            if (wave.hasPassedRobot(getRobot().getPoint(), getTime())) {
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
//        System.out.println(shadowing.getIntersectionFactor(wave, intersection));

        // test not averaging the values
//        value /= jBucket - iBucket + 1;

        return value;
    }

    public void drawWaves(Graphics2D gr) {
        final int WAVE_DIVISIONS = 400;

        G g = new G(gr);
        Wave earliestWave = waves.earliestFireWave(MyLog.getInstance().getLatest());

        for(Wave cur : waves) {
            if(!(cur instanceof EnemyFireWave))
                continue;

            EnemyFireWave wave = (EnemyFireWave) cur;

            g.drawCircle(wave.getSource(), wave.getDistanceTraveled(getTime()),
                    wave == earliestWave ? Color.WHITE : Color.BLUE);

            TargetingLog log = waves.getData(wave).getLog();
            GuessFactorStats st = waves.getData(wave).getStats();

            Point zeroPoint = wave.getSource().project(log.getZeroGf(), wave.getDistanceTraveled(getTime()));

            g.drawLine(wave.getSource(), zeroPoint, Color.GRAY);

            if(log.preciseIntersection != null) {
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
}
