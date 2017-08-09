package roborio.myself;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class MyLog {
    private static final MyLog  SINGLETON = new MyLog();
    private static final int    LOG_SIZE = 1000;

    private MyRobot[]           log;
    private int                 length;
    private int                 removed;

    private MyLog() {
        log = new MyRobot[LOG_SIZE];
        length = 0;
        removed = 0;
    }

    public static MyLog getInstance() {
        return SINGLETON;
    }

    private int realAt(int i) {
        int idx = (removed + i) % LOG_SIZE;
        if(idx < 0)
            idx += LOG_SIZE;
        return idx;
    }

    public MyRobot getLatest() {
        if(length == 0)
            return null;
        return log[realAt(length-1)];
    }

    public MyRobot getKthLatest(int k) {
        if(length < k)
            k = length;
        return log[realAt(length - k)];
    }

    public MyRobot atLeastAt(long time) {
        MyRobot res = null;
        for(int i = 1; i <= length; i++) {
            MyRobot cur = log[realAt(length - i)];
            if(cur.getTime() >= time)
                res = cur;
            else
                return res;
        }

        return res;
    }

    public MyRobot at(int i) {
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

    public MyRobot push(MyRobot me) {
        int newAhead = (int) Math.signum(me.getVelocity());
        if(newAhead == 0 && length > 0)
            newAhead = log[realAt(length - 1)].getAhead();

        me.setAhead(newAhead);

        log[realAt(length++)] = me;
        if(length > LOG_SIZE) {
            length = LOG_SIZE;
            removed++;
            if(removed >= LOG_SIZE)
                removed = 0;
        }

        return me;
    }

    public void pop() {
        if(length == 0)
            throw new ArrayIndexOutOfBoundsException("popping empty MyLog");

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

    public MySnapshot takeSnapshot(int index, int size) {
        if(index >= length)
            throw new ArrayIndexOutOfBoundsException();

        int low = Math.max(index - size + 1, 0);
        int high = Math.min(index + size, length);
        int baseIndex = index - low;

        MyRobot[] snap = new MyRobot[high - low];
        for(int i = low; i < high; i++) {
            snap[i - low] = log[realAt(i)];
        }

        return new MySnapshot(snap, baseIndex);
    }

    public MySnapshot takeSnapshot(int size) {
        if(length == 0)
            return null;
        return takeSnapshot(length - 1, size);
    }
}
