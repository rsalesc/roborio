package rsalesc.roborio.enemies;

import robocode.ScannedRobotEvent;
import rsalesc.roborio.utils.BackAsFrontRobot;

import java.util.HashMap;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class EnemyTracker {
    private static EnemyTracker SINGLETON = null;

    private HashMap<String, EnemyLog> seenEnemies;

    private EnemyTracker() {
        seenEnemies = new HashMap<>();
    }

    public int size() {
        return seenEnemies.size();
    }

    public static EnemyTracker getInstance() {
        if(SINGLETON == null)
            SINGLETON = new EnemyTracker();
        return SINGLETON;
    }

    public ComplexEnemyRobot push(ScannedRobotEvent e, BackAsFrontRobot from) {
        if(!seenEnemies.containsKey(e.getName())) {
            EnemyLog log = new EnemyLog();
            ComplexEnemyRobot res = log.push(e, from);
            seenEnemies.put(e.getName(), log);
            return res;
        } else {
            return seenEnemies.get(e.getName()).push(e, from);
        }
    }

    public EnemyLog getLog(String name) {
        return seenEnemies.get(name);
    }

    public EnemyLog getLog(ScannedRobotEvent e) {
        return getLog(e.getName());
    }

    public EnemyLog getLog(ComplexEnemyRobot e) {
        return getLog(e.getName());
    }

    public ComplexEnemyRobot getLatestState(String name) {
        return getLog(name).getLatest();
    }

    public ComplexEnemyRobot getLatestState(ScannedRobotEvent e) {
        return getLatestState(e.getName());
    }

    public ComplexEnemyRobot getLatestState(ComplexEnemyRobot e) {
        return getLatestState(e.getName());
    }

    public void clear() {
        seenEnemies.clear();
    }
}
