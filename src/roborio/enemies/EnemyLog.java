package roborio.enemies;

/**
 * Created by Roberto Sales on 21/07/17.
 */

import robocode.Robot;
import robocode.ScannedRobotEvent;

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

    public void push(ComplexEnemyRobot enemy) {
        log[realAt(length++)] = enemy;
        if(length > LOG_SIZE) {
            length = LOG_SIZE;
            removed++;
            if(removed >= LOG_SIZE)
                removed = 0;
        }
    }

    public void push(ScannedRobotEvent e, Robot from) {
        push(new ComplexEnemyRobot(e, from));
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
}
