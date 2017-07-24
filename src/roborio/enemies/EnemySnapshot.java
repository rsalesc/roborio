package roborio.enemies;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class EnemySnapshot {
    private ComplexEnemyRobot[] log;
    private int                 logSize;
    private int                 baseIndex;

    public EnemySnapshot(ComplexEnemyRobot[] snap) {
        log = snap;
        logSize = snap.length;
        baseIndex = 0;
    }

    public EnemySnapshot(ComplexEnemyRobot[] snap, int baseIndex) {
        this(snap);
        this.baseIndex = baseIndex;
    }

    private int realAt(int i) {
        return i + baseIndex;
    }

    public int size() {
        return logSize;
    }

    public ComplexEnemyRobot at(int i) {
        if(realAt(i) < 0 || realAt(i) >= logSize)
            throw new ArrayIndexOutOfBoundsException();

        return log[realAt(i)];
    }

    public ComplexEnemyRobot getOffset(int k) {
        int idx = Math.max(0, Math.min(logSize - 1, realAt(k)));
        return log[idx];
    }
}
