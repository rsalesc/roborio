package roborio.myself;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public class MySnapshot {
    private MyRobot[]           log;
    private int                 logSize;
    private int                 baseIndex;

    public MySnapshot(MyRobot[] snap) {
        log = snap;
        logSize = snap.length;
        baseIndex = 0;
    }

    public MySnapshot(MyRobot[] snap, int baseIndex) {
        this(snap);
        this.baseIndex = baseIndex;
    }

    private int realAt(int i) {
        return baseIndex + i;
    }

    public int size() {
        return logSize;
    }

    public MyRobot at(int i) {
        if(realAt(i) < 0 || realAt(i) >= logSize)
            throw new ArrayIndexOutOfBoundsException();
        return log[realAt(i)];
    }

    public MyRobot getOffset(int k) {
        int idx = Math.max(0, Math.min(logSize - 1, realAt(k)));
        return log[idx];
    }
}
