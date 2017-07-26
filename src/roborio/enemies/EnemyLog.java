package roborio.enemies;

/**
 * Created by Roberto Sales on 21/07/17.
 */

import robocode.ScannedRobotEvent;
import roborio.utils.BackAsFrontRobot;

/**
 * This class assumes that the enemy logs
 * will be added in increasing order of time
 */

public class EnemyLog {
    private static int              LOG_SIZE = 500;

    private ComplexEnemyRobot[]     log;
    private int                     length;
    private int                     removed;

    EnemyLog() {
        log = new ComplexEnemyRobot[LOG_SIZE];
        removed = 0;
        length = 0;
    }

    private int realAt(int i) {
        int idx = (removed + i) % LOG_SIZE;
        if(idx < 0)
            idx += LOG_SIZE;
        return idx;
    }

    public ComplexEnemyRobot getLatest() {
        if(length == 0)
            return null;
        return log[realAt(length-1)];
    }


    /**
     * @param k is 1-indexed
     * @return
     */
    public ComplexEnemyRobot getKthLatest(int k) {
        if(length < k)
            k = length;
        return log[realAt(length - k)];
    }

    /* TODO: put a binary search over here */
    public ComplexEnemyRobot atLeastAt(long time) {
        ComplexEnemyRobot res = null;
        for(int i = 1; i <= length; i++) {
            ComplexEnemyRobot cur = log[realAt(length - i)];
            if(cur.getTime() >= time)
                res = cur;
            else
                return res;
        }

        return res;
    }

    public ComplexEnemyRobot at(int i) throws ArrayIndexOutOfBoundsException {
        if(i >= length)
            throw new ArrayIndexOutOfBoundsException();
        return log[realAt(i)];
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public int size() {
        return length;
    }

    public ComplexEnemyRobot push(ComplexEnemyRobot enemy) {
        log[realAt(length++)] = enemy;
        if(length > LOG_SIZE) {
            length = LOG_SIZE;
            removed++;
            if(removed >= LOG_SIZE)
                removed = 0;
        }

        return enemy;
    }

    public ComplexEnemyRobot push(ScannedRobotEvent e, BackAsFrontRobot from) {
        return push(new ComplexEnemyRobot(e, from));
    }

    public void pop() {
        if(length == 0)
            throw new ArrayIndexOutOfBoundsException("popping empty EnemyLog");

        log[removed] = null;
        removed++;
        if(removed >= LOG_SIZE)
            removed = 0;
    }

    public void shrink(int count) {
        int discard = Math.max(0, length - count);
        for(int i = 0; i < discard; i++)
            pop();
    }

    public EnemySnapshot takeSnapshot(int index, int size) {
        if(index >= length)
            throw new ArrayIndexOutOfBoundsException();

        int low = Math.max(index - size + 1, 0);
        int high = Math.min(index + size, length);
        int baseIndex = index - low;

        ComplexEnemyRobot[] snap = new ComplexEnemyRobot[high - low];
        for(int i = low; i < high; i++) {
            snap[i] = log[realAt(i)];
        }

        return new EnemySnapshot(snap, baseIndex);
    }

    public EnemySnapshot takeSnapshot(int size) {
        if(length == 0)
            return null;
        return takeSnapshot(length - 1, size);
    }

    /*
        DELTAS
     */

    public double getEnergyDelta() {
        return getLatest().getEnergy() - getKthLatest(2).getEnergy();
    }
}
