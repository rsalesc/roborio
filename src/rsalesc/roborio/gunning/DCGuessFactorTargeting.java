package rsalesc.roborio.gunning;

import robocode.util.Utils;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;

import java.util.List;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public abstract class DCGuessFactorTargeting extends GuessFactorTargeting {
    private Double            _lastGf;

    public KnnSet<GuessFactorRange> knn;

    private Double               _lastFiringGf;
    private long                     _lastStatsTime = -1;
    private GuessFactorStats         _lastStats;
    private TargetingLog _lastMissLog;
    private TargetingLog _lastFiringLog;

    @Override
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, getKnnSet());
        }

        knn = (KnnSet) (store.get(storageHint));
    }

    public abstract KnnSet<GuessFactorRange> getKnnSet();

    public double generateFiringAngle(TargetingLog firingLog) {
        Range preciseMea = firingLog.getPreciseMea();

//        double bandwidth = Physics.hitAngle(firingLog.distance) * 0.4 / preciseMea.minAbsolute()
//                * GuessFactorStats.BUCKET_COUNT;

//        Smoothing smoother = new GaussianSmoothing(bandwidth);

//        GuessFactorStats gfRange = getStats(firingLog);
//        gfRange.setSmoother(smoother);

//        double bestGF = gfRange.getBestGuessFactor();
        double bestGF = getBestGF(firingLog);
        double bearingOffset = firingLog.getOffset(bestGF);

        double res = Utils.normalAbsoluteAngle(firingLog.absBearing
                + bearingOffset);

        _lastFiringLog = firingLog;
//        _lastStats = gfRange;
        _lastGf = bestGF;

        return res;
    }

    @Override
    public void log(TargetingLog missLog, boolean isVirtual) {
        if(!isVirtual) {
            _lastMissLog = missLog;
        }

        // process range gf
        double gfLow = missLog.getGfFromAngle(missLog.preciseIntersection.getStartingAngle());
        double gfHigh = missLog.getGfFromAngle(missLog.preciseIntersection.getEndingAngle());
        if(gfLow > gfHigh) {
            double tmp = gfLow;
            gfLow = gfHigh;
            gfHigh = tmp;
        }

        knn.add(missLog, new GuessFactorRange(gfLow, gfHigh));
    }

    // TODO: avoid knning again
    public GuessFactorStats getStats(TargetingLog f) {
        if(_lastStatsTime == f.time)
            return _lastStats;

        List<Knn.Entry<GuessFactorRange>> found = knn.query(f);

        GuessFactorStats stats = new GuessFactorStats(Double.POSITIVE_INFINITY);

        for(Knn.Entry<GuessFactorRange> entry : found) {
            double gf = entry.payload.getCenter();

            stats.logGuessFactor(gf, entry.weight);
        }

        Range preciseMea = f.getPreciseMea();

        double bandwidth = Physics.hitAngle(f.distance) * 0.4 / preciseMea.minAbsolute()
                * GuessFactorStats.BUCKET_COUNT;

        stats.setSmoother(new GaussianSmoothing(bandwidth));
        _lastStatsTime = f.time;
        return _lastStats = stats;
    }

    public double getBestGF(TargetingLog f) {
        List<Knn.Entry<GuessFactorRange>> found = knn.query(f);
        int length = found.size();

        double gf = 0;
        double gfHeight = -1;

        double[] candidates = new double[length];

        for(int i = 0; i < length; i++) {
            GuessFactorRange payload = found.get(i).payload;
            candidates[i] = (payload.min + payload.max) / 2;
        }

        for(int i = 0; i < length; i++) {
            double acc = 0;
            for(Knn.Entry<GuessFactorRange> entry : found) {
                GuessFactorRange range = entry.payload;
                double bandwidth = range.getRadius();
                double center = range.getCenter();

                acc += R.cubicKernel((candidates[i] - center) / bandwidth) * entry.weight; // mul by weight?
            }

            if(acc > gfHeight) {
                gfHeight = acc;
                gf = candidates[i];
            }
        }

        return gf;
    }

    @Override
    public Double getLastFiringFactor() {
        return _lastGf;
    }

    @Override
    public TargetingLog getLastFiringLog() {
        return _lastFiringLog;
    }

    @Override
    public TargetingLog getLastMissLog() {
        return _lastMissLog;
    }

    @Override
    public GuessFactorStats getLastStats() {
        if(_lastFiringLog == null)
            return null;
        return getStats(_lastFiringLog);
    }
}
