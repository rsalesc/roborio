package rsalesc.roborio.movement;

import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.LineSegment;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.waves.EnemyFireWave;
import rsalesc.roborio.utils.waves.Wave;

import java.util.*;

/**
 * Created by Roberto Sales on 09/08/17.
 * TODO: remove shadows of bullets which became inactive because of hit
 */
public class ShadowManager {
    private static final int THRESHOLD = 2000;

    private HashMap<Wave, ArrayList<Shadow>> shadows;
    private HashSet<VirtualBullet> bullets;

    public ShadowManager() {
        shadows = new HashMap<>();
        bullets = new HashSet<>();
    }

    public ArrayList<Shadow> getShadows(Wave wave) {
        if(!shadows.containsKey(wave))
            return new ArrayList<Shadow>();
        return shadows.get(wave);
    }

    /* remove shadows that were not cast yet */
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        Bullet bullet = e.getBullet();
        Point hitPoint = new Point(bullet.getX(), bullet.getY());

        for(Map.Entry<Wave, ArrayList<Shadow>> entry : shadows.entrySet()) {
            EnemyFireWave wave = (EnemyFireWave) entry.getKey();
            Iterator<Shadow> sit = entry.getValue().iterator();
            while(sit.hasNext()) {
                Shadow shadow = sit.next();
                if(!wave.getCircle(e.getTime()).isInside(hitPoint)
                        && shadow.bullet.getBullet().equals(bullet)
                        && !wave.wasFiredBy(bullet, e.getTime())) {
                    sit.remove();
                }
            }
        }
    }

    public void push(EnemyFireWave[] waves) {
        cleanup(new HashSet<>(Arrays.asList(waves)), null);
        for(Wave wave : waves) {
            if(!shadows.containsKey(wave)) {
                shadows.put(wave, new ArrayList<Shadow>());
                for(VirtualBullet bullet : bullets) {
                    computeShadow(wave, bullet);
                }
            }
        }
    }

    public void push(VirtualBullet[] newBullets) {
        cleanup(null, new HashSet<>(Arrays.asList(newBullets)));
        for(VirtualBullet bullet : newBullets) {
            if(!bullets.contains(bullet)) {
                for(Map.Entry<Wave, ArrayList<Shadow>> entry : shadows.entrySet()) {
                    Wave wave = entry.getKey();
                    computeShadow(wave, bullet);
                }

                bullets.add(bullet);
            }
        }
    }

    private void computeShadow(Wave wave, VirtualBullet bullet) {
        ArrayList<Shadow> shades = shadows.get(wave);
        AngularRange range = getShadow(wave, bullet);
        if(range == null)
            return;
        shades.add(new Shadow(range.getAngle(0), range, bullet));
    }

//    public static AngularRange getShadow(Wave wave, VirtualBullet bullet) {
//        long time = Math.max(bullet.getTime(), wave.getTime());
//        if(wave.getSource().distance(bullet.project(time)) < wave.getDistanceTraveled(time) - R.EPSILON)
//            return null;
//
//        int iterations = 0;
//        while(wave.getSource().distance(bullet.project(time)) > wave.getDistanceTraveled(time) && iterations++ < 120) {
//            time++;
//        }
//
//        if(iterations > 120)
//            return null;
//
//        LineSegment segment = new LineSegment(bullet.project(time - 1), bullet.project(time));
//        Point[] interOuter = segment.intersect(wave.getCircle(time));
//        Point[] interInner = segment.intersect(wave.getCircle(time - 1));
//
//        ArrayList<Point> candidates = new ArrayList<>();
//        for(Point point : interOuter)
//            candidates.add(point);
//
//        for(Point point : interInner)
//            candidates.add(point);
//
//        if(wave.getCircle(time).isInside(segment.p1) && !wave.getCircle(time - 1).isInside(segment.p1))
//            candidates.add(segment.p1);
//
//        if(wave.getCircle(time).isInside(segment.p2) && !wave.getCircle(time - 1).isInside(segment.p2))
//            candidates.add(segment.p2);
//
//        Point mean = segment.middle();
//        double absBearing = Physics.absoluteBearing(wave.getSource(), mean);
//
//        Range range = new Range();
//
//        for(Point point : candidates) {
//            double offset = Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), point) - absBearing);
//            range.push(offset);
//        }
//
//        return new AngularRange(absBearing, range);
//    }

    public static AngularRange getShadow(Wave wave, VirtualBullet bullet) {
        LineSegment intersection = getShadowSegment(wave, bullet);
        if(intersection == null)
            return null;

        Point middle = intersection.middle();

        double absBearing = Physics.absoluteBearing(wave.getSource(), middle);
        AngularRange range = new AngularRange(absBearing, -1e-8, +1e-8);

        range.pushAngle(Physics.absoluteBearing(wave.getSource(), intersection.p1));
        range.pushAngle(Physics.absoluteBearing(wave.getSource(), intersection.p2));

        return range;
    }

    public static LineSegment getShadowSegment(Wave wave, VirtualBullet bullet) {
        long time = Math.max(bullet.getTime(), wave.getTime());
        if(wave.getSource().distance(bullet.project(time)) < wave.getDistanceTraveled(time) - R.EPSILON)
            return null;

        int iterations = 0;
        while(wave.getSource().distance(bullet.project(time)) > wave.getDistanceTraveled(time) && iterations++ < 120) {
            time++;
        }

        if(iterations > 120)
            return null;

        LineSegment segment = new LineSegment(bullet.project(time - 1), bullet.project(time));

        Point pA;
        Point pB;

        if(wave.getCircle(time - 1).isInside(segment.p2))
            pB = segment.rayLikeIntersect(wave.getCircle(time - 1));
        else
            pB = segment.p2;

        if(wave.getCircle(time).isInside(segment.p1))
            pA = segment.p1;
        else
            pA = segment.rayLikeIntersect(wave.getCircle(time));

        return new LineSegment(pA, pB);
    }

    private void cleanup(HashSet<Wave> newWaves, HashSet<VirtualBullet> newBullets) {
        if(newBullets != null) {
            Iterator<VirtualBullet> iterator = bullets.iterator();
            while(iterator.hasNext()) {
                VirtualBullet bullet = iterator.next();
                if(!newBullets.contains(bullet))
                    iterator.remove();
            }
        }

        if(newWaves != null) {
            Iterator<Map.Entry<Wave, ArrayList<Shadow>>> iterator = shadows.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<Wave, ArrayList<Shadow>> entry = iterator.next();
                Wave wave = entry.getKey();
                if(!newWaves.contains(wave)) {
                    iterator.remove();
                }
            }
        }
    }

    public double getIntersectionFactor(Wave wave, AngularRange intersection) {
        if(!shadows.containsKey(wave))
            return 0.0;

        AngularRange whole = new AngularRange(intersection.getAngle(0), intersection.min, intersection.max);
        ArrayList<AngularRange> pieces = new ArrayList<>();
        pieces.add(whole);

        for(Shadow shadow : shadows.get(wave)) {
            ArrayList<AngularRange> nextPieces = new ArrayList<>();
            AngularRange shadowRange = shadow.range;

            for(AngularRange piece : pieces) {
                AngularRange shadowIntersection = piece.intersectAngles(shadowRange);
                if(R.isNear(shadowIntersection.getLength(), 0))
                    nextPieces.add(piece);
                else {
                    nextPieces.add(new AngularRange(piece.getAngle(0), piece.min, shadowIntersection.min));
                    nextPieces.add(new AngularRange(piece.getAngle(0), shadowIntersection.max, piece.max));
                }
            }

            pieces = nextPieces;
        }

        double totalLength = 0;
        for(AngularRange range : pieces)
            totalLength += range.getLength();

        double res = totalLength / intersection.getLength();
        if(Double.isNaN(res))
            return 1.0;

        return 1.0 - res;
    }
}
