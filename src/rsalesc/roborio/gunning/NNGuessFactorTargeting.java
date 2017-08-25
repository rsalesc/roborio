package rsalesc.roborio.gunning;

import robocode.util.Utils;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.nn.GuessFactorNetwork;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.stats.smoothing.GaussianSmoothing;
import rsalesc.roborio.utils.stats.smoothing.NoSmoothing;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.waves.BreakType;

import java.util.LinkedList;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public abstract class NNGuessFactorTargeting extends GuessFactorTargeting {
    private TargetingLog _lastFiringLog;
    private TargetingLog _lastMissLog;
    private Double       _lastGf;
    private GuessFactorStats _lastStats;
    private long             _lastStatsTime;

    private GuessFactorNetwork nn;
    private LinkedList<TargetingLog> fireLogs;

    protected abstract GuessFactorNetwork getNetwork();

    @Override
    public GuessFactorStats getLastStats() {
        if(_lastFiringLog != null && _lastFiringLog.time > _lastStatsTime) {
            _lastStatsTime = _lastFiringLog.time;
            return getStats(_lastFiringLog);
        } else {
            return _lastStats;
        }
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
    protected void buildStructure() {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, getNetwork());
        }

        if(!store.contains(storageHint + "-list")) {
            store.add(storageHint + "-list", new LinkedList<TargetingLog>());
        }

        nn = (GuessFactorNetwork) (store.get(storageHint));
        fireLogs = (LinkedList) (store.get(storageHint + "-list"));
    }

    @Override
    public double generateFiringAngle(TargetingLog firingLog) {
        double bestGF = getBestGF(firingLog);
        double bearingOffset = firingLog.getOffset(bestGF);

        double res = Utils.normalAbsoluteAngle(firingLog.absBearing
                + bearingOffset);

        _lastFiringLog = firingLog;
        _lastGf = bestGF;

        return res;
    }

    @Override
    public void log(TargetingLog missLog, BreakType type) {
        if(type != BreakType.VIRTUAL_BREAK) {
            _lastMissLog = missLog;
        } else {
            int lucky = (int) (Math.random() * fireLogs.size() - 1e-10);
            TargetingLog luckyGuy = fireLogs.get(lucky);
            GuessFactorRange luckyRange = getGfRange(luckyGuy);
            nn.setSmoother(new GaussianSmoothing(luckyRange.getRadius() * 0.75))
                    .setLearningRate(0.75)
                    .train(luckyGuy, luckyRange.getCenter());

            return;
        }

        if(type == BreakType.BULLET_HIT) {
            fireLogs.add(missLog);
            while (fireLogs.size() > 250)
                fireLogs.pop();
        }

        nn.setLearningRate(0.15);
        for(int i = 0; i < 1; i++) {
            for(int j = 0; j < 5; j++) {
                if (j >= fireLogs.size()) break;
                TargetingLog f = fireLogs.get(j);
                GuessFactorRange range = getGfRange(f);

                // do not train if hit?
                nn.setSmoother(new GaussianSmoothing(range.getRadius() * 0.75))
                        .train(f, range.getCenter());
            }
        }
    }

    private GuessFactorRange getGfRange(TargetingLog missLog) {
        // process range gf
        double gfLow = missLog.getGfFromAngle(missLog.preciseIntersection.getStartingAngle());
        double gfHigh = missLog.getGfFromAngle(missLog.preciseIntersection.getEndingAngle());
        if(gfLow > gfHigh) {
            double tmp = gfLow;
            gfLow = gfHigh;
            gfHigh = tmp;
        }

        GuessFactorRange range = new GuessFactorRange(gfLow, gfHigh);
        return range;
    }

    private double getBestGF(TargetingLog f) {
        return nn.getBestGuessFactor(f);
    }

    private GuessFactorStats getStats(TargetingLog f) {
        double[] buffer = nn.feed(f);
        double[] longerBuffer = new double[GuessFactorStats.BUCKET_COUNT];

        int mid = GuessFactorNetwork.BUCKET_MID;

        for(int i = 0; i < longerBuffer.length; i++) {
            double gf = (double) (i - GuessFactorStats.BUCKET_MID) / GuessFactorStats.BUCKET_MID;
            int buck = (int)(gf * mid) + mid;
            longerBuffer[i] = buffer[buck];
        }

        _lastStats = new GuessFactorStats(longerBuffer, Double.POSITIVE_INFINITY);
        _lastStats.setSmoother(new NoSmoothing());

        return _lastStats;
    }
}
