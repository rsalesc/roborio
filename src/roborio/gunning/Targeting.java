package roborio.gunning;

import roborio.gunning.utils.TargetingLog;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public abstract class Targeting {
    public abstract double generateFiringAngle(TargetingLog firingLog);
    public abstract void log(TargetingLog missLog, boolean isVirtual);
}
