package roborio.utils.stats;

import roborio.utils.stats.smoothing.NoSmoothing;
import roborio.utils.stats.smoothing.Smoothing;

/**
 * Created by Roberto Sales on 27/07/17.
 */
public abstract class VCS extends Stats {
    private final double rolling;
    private final double factor;
    protected double[] smoothed;

    private Smoothing smoother;

    public VCS(int bins, double rollingDepth) {
        super(bins);
        this.rolling = rollingDepth;
        factor = 1.0 - 1.0 / (rolling + 1);
        smoothed = null;
        smoother = new NoSmoothing();
    }

    public void setSmoother(Smoothing smoother) {
        this.smoother = smoother;
        smoothed = null;
    }

    public Smoothing getSmoother() {
        return smoother;
    }

    public double getRollingDepth() {
        return rolling;
    }

    private void unroll() {
        for(int i = 0; i < buffer.length; i++)
            buffer[i] *= factor;
    }

    public void add(int i, double weight) {
        unroll();
        buffer[i] += weight;
        smoothed = null;
    }

    public void add(int i) {
        add(i, 1);
    }

    public double get(int i) {
        if(smoothed == null)
            smooth();

        return smoothed[i];
    }

    public double getFlat(int i) {
        return buffer[i];
    }

    protected void smooth() {
        smoothed = smoother.smooth(buffer);
    }
}
