package rsalesc.roborio.utils.waves;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.stats.GuessFactorStats;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class WaveSnap {
    private final TargetingLog log;
    private final GuessFactorStats stats;

    public WaveSnap(TargetingLog log, GuessFactorStats stats) {
        this.log = log;
        this.stats = stats;
    }

    public GuessFactorStats getStats() {
        return stats;
    }

    public TargetingLog getLog() {
        return log;
    }
}
