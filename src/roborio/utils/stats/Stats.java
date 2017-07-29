package roborio.utils.stats;

import roborio.utils.R;

/**
 * Created by Roberto Sales on 27/07/17.
 */
public class Stats {
    protected double[] buffer;

    public Stats(int bins) {
        buffer = new double[bins];
    }

    protected double get(int i) {
        return buffer[i];
    }

    protected void set(int i, double value) {
        buffer[i] = value;
    }

    public int size() {
        return buffer.length;
    }

    public void normalize() {
        double max = 0;
        for(int i = 0; i < buffer.length; i++) {
            max = Math.max(max, buffer[i]);
        }

        if(R.isNear(max, 0))
            return;

        for(int i = 0; i < buffer.length; i++)
            buffer[i] /= max;
    }
}
