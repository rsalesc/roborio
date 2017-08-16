package rsalesc.roborio.gunning;

import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.structures.Knn;
import rsalesc.roborio.structures.KnnSet;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.stats.smoothing.Smoothing;
import rsalesc.roborio.utils.storage.NamedStorage;

import java.util.List;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public abstract class DCGuessFactorTargeting extends Targeting {
    public Range _lastEscapeAngle;
    public Double            _lastGf;

    public KnnSet<Double> knn;

    public Double               _lastFiringGf;
    public ComplexEnemyRobot _lastEnemy;
    public GuessFactorStats         _lastStats;
    public TargetingLog _lastMissLog;

    @Override
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, getKnnSet());
        }

        knn = (KnnSet) (store.get(storageHint));
    }

    public abstract KnnSet<Double> getKnnSet();

    public double generateFiringAngle(TargetingLog firingLog) {
        Range preciseMea = firingLog.getPreciseMea();

        double bandwidth = Physics.hitAngle(firingLog.distance) * 0.4 / preciseMea.minAbsolute()
                * GuessFactorStats.BUCKET_COUNT;

        Smoothing smoother = new GaussianSmoothing(bandwidth);

        GuessFactorStats gfRange = getStats(firingLog);
        gfRange.setSmoother(smoother);

        double bestGF = gfRange.getBestGuessFactor();
        double bearingOffset = firingLog.getOffset(bestGF);

        double res = Utils.normalAbsoluteAngle(firingLog.absBearing
                + bearingOffset);

        _lastEscapeAngle = preciseMea;
        _lastStats = gfRange;
        _lastGf = bestGF;

        return res;
    }

    @Override
    public void log(TargetingLog missLog, boolean isVirtual) {
        double offset = Utils.normalRelativeAngle(missLog.hitAngle - missLog.absBearing);

        double gf = missLog.getGf(offset);

        if(!isVirtual) {
            _lastMissLog = missLog;
            _lastFiringGf = gf;
        }

        knn.add(missLog, gf);
    }


    public GuessFactorStats getStats(TargetingLog f) {
        List<Knn.Entry<Double>> found = knn.query(f);

        GuessFactorStats stats = new GuessFactorStats(Double.POSITIVE_INFINITY);
        double distanceSum = 1e-9;
        for(Knn.Entry<Double> entry : found) {
            distanceSum += entry.distance;
        }

        for(Knn.Entry<Double> entry : found) {
            double gf = entry.payload;

            double diff = entry.distance * found.size() / distanceSum;
            double weight = R.exp(-0.25 * diff * diff) * entry.weight;

            stats.logGuessFactor(gf, weight);
        }

        return stats;
    }
}
