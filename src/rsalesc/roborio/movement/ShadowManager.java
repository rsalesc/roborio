package rsalesc.roborio.movement;

import robocode.util.Utils;
import rsalesc.roborio.gunning.utils.VirtualBullet;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.LineSegment;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
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

    public void push(Wave[] waves) {
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
        long time = Math.max(bullet.getTime(), wave.getTime());
        if(wave.getSource().distance(bullet.project(time)) < wave.getDistanceTraveled(time) - R.EPSILON)
            return;

        double lastDiff = Double.POSITIVE_INFINITY;
        while(wave.getSource().distance(bullet.project(time)) > wave.getDistanceTraveled(time)) {
            double diff = wave.getSource().distance(bullet.project(time)) - wave.getDistanceTraveled(time);
            if(diff > lastDiff)
                return;

            lastDiff = diff;
            time++;
        }

        LineSegment segment = new LineSegment(bullet.project(time - 1), bullet.project(time));
        Point[] interOuter = segment.intersect(wave.getCircle(time));
        Point[] interInner = segment.intersect(wave.getCircle(time - 1));

        ArrayList<Point> candidates = new ArrayList<>();
        for(Point point : interOuter)
            candidates.add(point);

        for(Point point : interInner)
            candidates.add(point);

        if(wave.getCircle(time).isInside(segment.p1) && !wave.getCircle(time - 1).isInside(segment.p1))
            candidates.add(segment.p1);

        if(wave.getCircle(time).isInside(segment.p2) && !wave.getCircle(time - 1).isInside(segment.p2))
            candidates.add(segment.p2);

        Point mean = segment.middle();
        double absBearing = Physics.absoluteBearing(wave.getSource(), mean);

        Range range = new Range();

        for(Point point : candidates) {
            double offset = Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), point) - absBearing);
            range.push(offset);
        }

        ArrayList<Shadow> shades = shadows.get(wave);
        shades.add(new Shadow(absBearing, range));
    }

    private void cleanup(HashSet<Wave> newWaves, HashSet<VirtualBullet> newBullets) {
        if(newWaves != null) {
            Iterator<Map.Entry<Wave, ArrayList<Shadow>>> iterator = shadows.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<Wave, ArrayList<Shadow>> entry = iterator.next();
                Wave wave = entry.getKey();
                if(!newWaves.contains(wave))
                    iterator.remove();
            }
        }

        if(newBullets != null) {
            Iterator<VirtualBullet> iterator = bullets.iterator();
            while(iterator.hasNext()) {
                VirtualBullet bullet = iterator.next();
                if(!newBullets.contains(bullet))
                    iterator.remove();
            }
        }
    }
}
