package rsalesc.roborio.utils.waves;

import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.geo.Point;

import java.util.*;

/**
 * Created by Roberto Sales on 23/07/17.
 * TODO: fix memory leak
 */
public class WaveCollection implements Iterable<Wave> {
    protected List<Wave> waves;

    public WaveCollection() {
        waves = new LinkedList<Wave>();
    }

    public void add(Wave wave) {
        waves.add(wave);
    }

    public Wave[] earliestWaves(int K, Point dest, long time, WaveCondition condition) {
        K = Math.min(K, waves.size());
        ArrayList<Wave> res = new ArrayList<>();
        HashSet<Wave> seen = new HashSet<>();
        for(int i = 0; i < K; i++) {
            double earliestTime = Double.POSITIVE_INFINITY;
            Wave earliest = null;

            for (Wave wave : waves) {
                if (!seen.contains(wave) && condition.test(wave)) {
                    double breakAt = wave.getBreakTime(dest);
                    if (breakAt > time && breakAt < earliestTime) {
                        earliestTime = breakAt;
                        earliest = wave;
                    }
                }
            }

            if(earliest != null) {
                seen.add(earliest);
                res.add(earliest);
            }
        }

        return res.toArray(new Wave[0]);
    }

    public Wave earliestWave(Point dest, long time, WaveCondition condition) {
        Wave[] res = earliestWaves(1, dest, time, condition);
        if(res.length == 0)
            return null;
        return res[0];
    }

    public void remove(Wave wave) {
        waves.remove(wave);
    }

    public Wave earliestWave(Point dest, long time) {
        return earliestWave(dest, time, new WaveCondition.Tautology());
    }

    public Wave earliestWave(MyRobot robot) {
        return earliestWave(robot.getPoint(), robot.getTime());
    }

    public Wave earliestWave(ComplexEnemyRobot robot) {
        return earliestWave(robot.getPoint(), robot.getTime());
    }

    public Wave earliestFireWave(Point dest, long time) {
        return earliestWave(dest, time, new EnemyFireWaveCondition());
    }

    public Wave earliestFireWave(ComplexEnemyRobot robot) {
        return earliestFireWave(robot.getPoint(), robot.getTime());
    }

    public Wave earliestFireWave(MyRobot robot) {
        return earliestFireWave(robot.getPoint(), robot.getTime());
    }

    public Wave earliestFireWave(Point dest, long time, ComplexEnemyRobot from) {
        return earliestWave(dest, time, new EnemyFireWaveCondition(from.getName()));
    }

    public Wave earliestFireWave(ComplexEnemyRobot robot, ComplexEnemyRobot from) {
        return earliestFireWave(robot.getPoint(), robot.getTime(), from);
    }

    public Wave earliestFireWave(MyRobot robot, ComplexEnemyRobot from) {
        return earliestFireWave(robot.getPoint(), robot.getTime(), from);
    }

    public int removePassed(MyRobot robot) {
        int removed = 0;
        Iterator<Wave> it = iterator();
        while(it.hasNext()) {
            Wave wave = it.next();
            if(wave.hasPassedRobot(robot)) {
                it.remove();
                removed++;
            }
        }

        return removed;
    }

    public EnemyFireWave findImaginaryWave(long time) {
        for(Wave wave : waves) {
            if(wave instanceof EnemyFireWave) {
                EnemyFireWave fireWave = (EnemyFireWave) wave;
                if(fireWave.isImaginary() && fireWave.getTime() == time)
                    return fireWave;
            }
        }

        return null;
    }

    public int size() {
        return waves.size();
    }

    @Override
    public Iterator<Wave> iterator() {
        return waves.iterator();
    }

    public Wave[] toArray() {
        return waves.toArray(new Wave[0]);
    }

    public static class EnemyFireWaveCondition extends WaveCondition {
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
