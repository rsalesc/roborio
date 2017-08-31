package rsalesc.roborio.energy;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import rsalesc.roborio.enemies.ComplexEnemyRobot;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roberto Sales on 29/08/17.
 * TODO: test heat tracker, seems to have a flaw looking visually. gonna tackle it
 */
public class HeatTracker {
    private static final HeatTracker SINGLETON = new HeatTracker();

    double coolingRate = 0.1;

    private HashMap<String, HeatLog> logs;

    private HeatTracker() {
        logs = new HashMap<>();
    }

    public static HeatTracker getInstance() {
        return SINGLETON;
    }

    public void tick(long time) {
        for(Map.Entry<String, HeatLog> entry : logs.entrySet()) {
            entry.getValue().tick(time);
        }
    }

    public HeatLog ensure(String name) {
        if(!logs.containsKey(name)) {
            HeatLog log = new HeatLog();
            log.setCoolingRate(coolingRate);
            logs.put(name, log);
            return log;
        }
        return logs.get(name);
    }

    public void setCoolingRate(double x) {
        coolingRate = x;
    }

    public HeatLog ensure(ComplexEnemyRobot e) {
        return ensure(e.getName());
    }

    public void push(ComplexEnemyRobot e) {
        ensure(e).push(e);
    }

    public void onBulletHit(BulletHitEvent e) {
        ensure(e.getName()).onBulletHit(e);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        ensure(e.getName()).onHitByBullet(e);
    }
}
