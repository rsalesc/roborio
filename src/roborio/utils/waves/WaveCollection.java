package roborio.utils.waves;

import roborio.enemies.ComplexEnemyRobot;
import roborio.myself.MyRobot;
import roborio.utils.Point;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class WaveCollection {
    private List<Wave> waves;

    public WaveCollection() {
        waves = new LinkedList<Wave>();
    }

    public List<Wave> getWaves() {
        return waves;
    }

    public void add(Wave wave) {
        waves.add(wave);
    }

    public Wave earliestWave(Point dest, WaveCondition condition) {
        double earliestTime = Double.POSITIVE_INFINITY;
        Wave earliest = null;

        for(Wave wave : waves) {
            if(condition.test(wave)) {
                double breakAt = wave.getBreakTime(dest);
                if (breakAt < earliestTime) {
                    earliestTime = breakAt;
                    earliest = wave;
                }
            }
        }

        return earliest;
    }

    public Wave earliestWave(Point dest) {
        return earliestWave(dest, new WaveCondition.Tautology());
    }

    public Wave earliestWave(MyRobot robot) {
        return earliestWave(robot.getPoint());
    }

    public Wave earliestWave(ComplexEnemyRobot robot) {
        return earliestWave(robot.getPoint());
    }

    public Wave earliestFireWave(Point dest) {
        return earliestWave(dest, new EnemyFireWaveCondition());
    }

    public Wave earliestFireWave(ComplexEnemyRobot robot) {
        return earliestFireWave(robot.getPoint());
    }

    public Wave earliestFireWave(MyRobot robot) {
        return earliestWave(robot.getPoint());
    }

    public Wave earliestFireWave(Point dest, ComplexEnemyRobot from) {
        return earliestWave(dest, new EnemyFireWaveCondition(from.getName()));
    }

    public Wave earliestFireWave(ComplexEnemyRobot robot, ComplexEnemyRobot from) {
        return earliestFireWave(robot.getPoint(), from);
    }

    public Wave earliestFireWave(MyRobot robot, ComplexEnemyRobot from) {
        return earliestFireWave(robot.getPoint(), from);
    }

    public int removePassed(MyRobot robot) {
        int removed = 0;
        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasPassedRobot(robot)) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    private static class EnemyFireWaveCondition extends WaveCondition {
        private String name;
        public EnemyFireWaveCondition() {
            name = null;
        }

        public EnemyFireWaveCondition(String name) {
            this.name = name;
        }

        @Override
        public boolean test(Wave wave) {
            if(name == null)
                return wave.isReal();
            return wave.isReal() && wave.isEnemyWave() && ((EnemyFireWave)wave).isFrom(name);
        }
    }
}
