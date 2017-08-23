package rsalesc.roborio.gunning;

import robocode.util.Utils;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.stats.GuessFactorStats;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public abstract class GuessFactorTargeting extends Targeting {
    public abstract GuessFactorStats getLastStats();
    public abstract Double getLastFiringFactor();
    public abstract TargetingLog getLastFiringLog();
    public abstract TargetingLog getLastMissLog();

    public Range getLastEscapeAngle() {
        TargetingLog f = getLastFiringLog();
        if(f == null)
            return null;
        if(f.getPreciseMea() != null)
            return f.getPreciseMea();
        return new Range(-f.getMea(), +f.getMea());
    }

    public double getLastMissFactor() {
        TargetingLog missLog = getLastMissLog();
        if(missLog == null)
            return 0.0;
        double offset = Utils.normalRelativeAngle(missLog.hitAngle - missLog.absBearing);
        double gfBreak = missLog.getGf(offset);
        return gfBreak;
    }
}
