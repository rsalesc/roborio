package roborio.enemies;

import robocode.Robot;
import robocode.ScannedRobotEvent;

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

    public void push(ScannedRobotEvent e, Robot from) {
        if(!seenEnemies.containsKey(e.getName())) {
            EnemyLog log = new EnemyLog();
            log.push(e, from);
            seenEnemies.put(e.getName(), log);
        } else {
            seenEnemies.get(e.getName()).push(e, from);
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
