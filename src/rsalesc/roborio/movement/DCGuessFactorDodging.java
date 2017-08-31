package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.waves.BreakType;

import java.util.List;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public abstract class DCGuessFactorDodging extends GuessFactorDodging {
    private GuessFactorStats lastStats;
    private KnnSet<GuessFactorRange> knn;

    public abstract KnnSet<GuessFactorRange> getNewKnnSet();

    @Override
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, getNewKnnSet());
        }

        knn = (KnnSet) (store.get(storageHint));
    }

    @Override
    public boolean hasData(Knn.ParametrizedCondition condition) {
        return knn.availableData(condition) > 0;
    }

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

        knn.add(missLog, new GuessFactorRange(gfLow, gfHigh), type);
    }

    @Override
    public void tick(long time, int roundNum) {
        knn.mutate(new Knn.ConditionMutation(time, roundNum));
    }

    @Override
    public GuessFactorStats getStats(TargetingLog f, Knn.ParametrizedCondition condition) {
        List<Knn.Entry<GuessFactorRange>> found = knn.query(f, condition);

        GuessFactorStats stats = new GuessFactorStats(Double.POSITIVE_INFINITY);

        for(Knn.Entry<GuessFactorRange> entry : found) {
            double gf = entry.payload.getCenter();

            stats.logGuessFactor(gf, entry.weight);
        }

        Range preciseMea = f.getPreciseMea();

        double bandwidth = Physics.hitAngle(f.distance) / preciseMea.minAbsolute() * 0.42
                * GuessFactorStats.BUCKET_COUNT;

        stats.setSmoother(new GaussianSmoothing(bandwidth));
//        stats.setSmoother(new NoSmoothing());
        return lastStats = stats;
    }
}
