package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.stats.GuessFactorStats;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.waves.BreakType;

/**
 * Created by Roberto Sales on 20/08/17.
 */
public abstract class GuessFactorDodging {
    private boolean built = false;
    protected String storageHint;

    public GuessFactorDodging setIdentifier(String id) {
        this.storageHint = id;
        return this;
    }

    public String getIdentifier() {
        return storageHint;
    }

    public abstract GuessFactorStats getLastStats();
    public abstract void log(TargetingLog log, BreakType type);
    public abstract GuessFactorStats getStats(TargetingLog log, Knn.ParametrizedCondition condition);
    public abstract void tick(long time, int roundNum);
    protected abstract void buildStructure();

    public boolean isBuilt() {
        return built;
    }

    public GuessFactorDodging build() {
        built = true;
        buildStructure();
        return this;
    }
}
