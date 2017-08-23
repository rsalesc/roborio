package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.SegmentedBufferSet;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.waves.BreakType;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public abstract class VCSGuessFactorDodging extends GuessFactorDodging {
    private GuessFactorStats lastStats;
    private SegmentedBufferSet buffer;

    public abstract SegmentedBufferSet getBufferSet();

    @Override
    public GuessFactorStats getLastStats() {
        return lastStats;
    }

    @Override
    public void log(TargetingLog missLog, BreakType type) {
        // process range gf
        double gfLow = missLog.getGfFromAngle(missLog.preciseIntersection.getStartingAngle());
        double gfHigh = missLog.getGfFromAngle(missLog.preciseIntersection.getEndingAngle());
        if(gfLow > gfHigh) {
            double tmp = gfLow;
            gfLow = gfHigh;
            gfHigh = tmp;
        }

        GuessFactorRange range = new GuessFactorRange(gfLow, gfHigh);
        buffer.logGuessFactor(missLog, range.getCenter(), type);
    }

    @Override
    public GuessFactorStats getStats(TargetingLog log, double confidence, int roundNum) {
        GuessFactorStats stats = buffer.getStats(log, new Knn.HitLeastCondition(confidence, roundNum));
        Range preciseMea = log.getPreciseMea();

        double bandwidth = Physics.hitAngle(log.distance) / preciseMea.minAbsolute() * 0.42
                * GuessFactorStats.BUCKET_COUNT;

        stats.setSmoother(new GaussianSmoothing(bandwidth));
        return lastStats = stats;
    }

    @Override
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, getBufferSet());
        }

        buffer = (SegmentedBufferSet) (store.get(storageHint));
    }
}
