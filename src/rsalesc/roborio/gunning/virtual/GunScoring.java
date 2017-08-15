package rsalesc.roborio.gunning.virtual;

import rsalesc.roborio.gunning.VirtualGunArray;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public abstract class GunScoring {
    private String storageHint;

    public abstract void fire(int index, double power);
    public abstract void log(int index, double alpha, double power);
    public abstract double[] evaluate(VirtualGunArray array);

    public GunScoring setIdentifier(String name) {
        this.storageHint = name;
        return this;
    }

    public String getIdentifier() {
        return storageHint;
    }
}
