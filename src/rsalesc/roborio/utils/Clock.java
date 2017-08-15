package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 12/08/17.
 */
public class Clock {
    private final double start;
    public Clock() {
        this.start = System.nanoTime() / 1e6;
    }

    public double spent() {
        return System.nanoTime() / 1e6 - start;
    }
}
