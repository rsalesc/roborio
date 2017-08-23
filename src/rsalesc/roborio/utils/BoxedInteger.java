package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class BoxedInteger {
    private long value;

    public BoxedInteger() {
        value = 0;
    }

    public BoxedInteger(long x) {
        value = x;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public long toLong() {
        return (long) value;
    }

    public double toDouble() {
        return (double) value;
    }
}
