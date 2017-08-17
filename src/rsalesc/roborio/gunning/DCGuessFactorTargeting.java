package rsalesc.roborio.gunning;

import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.storage.NamedStorage;

import java.util.List;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public abstract class DCGuessFactorTargeting extends Targeting {
    public Range _lastEscapeAngle;
    public Double            _lastGf;

    public KnnSet<GuessFactorRange> knn;

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

        _lastEscapeAngle = preciseMea;
//        _lastStats = gfRange;
        _lastGf = bestGF;

        return res;
    }

    @Override
    public void log(TargetingLog missLog, boolean isVirtual) {
        double offset = Utils.normalRelativeAngle(missLog.hitAngle - missLog.absBearing);
        double gfBreak = missLog.getGf(offset);

        if(!isVirtual) {
            _lastMissLog = missLog;
            _lastFiringGf = gfBreak;
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


//    public GuessFactorStats getStats(TargetingLog f) {
//        List<Knn.Entry<GuessFactorRange>> found = knn.query(f);
//        int length = found.size();
//
//        double gf = 0;
//        double gfHeight = -1;
//
//        double[] candidates = new double[length * 2];
//
//        for(int i = 0; i < length; i++) {
//            candidates[i<<1] = found.get(i).payload.min;
//            candidates[i<<1|1] = found.get(i).payload.max;
//        }
//
//        Arrays.sort(candidates);
//
//        for(int i = 0; i < 2*length; i++) {
//
//        }
//
//        return stats;
//    }

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

                acc += R.cubicKernel((candidates[i] - center) / bandwidth);
            }

            if(acc > gfHeight) {
                gfHeight = acc;
                gf = candidates[i];
            }
        }

        return gf;
    }
}
