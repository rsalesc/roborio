package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.waves.BreakType;

import java.awt.*;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public abstract class Targeting {
    protected String storageHint;
    protected boolean built;

    public Targeting setIdentifier(String id) {
        this.storageHint = id;
        return this;
    }

    public String getIdentifier() {
        return storageHint;
    }

    protected abstract void buildStructure();
    public abstract double generateFiringAngle(TargetingLog firingLog);
    public abstract void log(TargetingLog missLog, BreakType type);

    public Targeting build() {
        buildStructure();
        built = true;
        return this;
    }

    public boolean isBuilt() {
        return built;
    }

    public void onPaint(Graphics2D g) {}
}
