package rsalesc.roborio.movement;

import rsalesc.roborio.utils.structures.Knn;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class LateFlatteningCondition extends Knn.HitLeastCondition {
    public int lateRound = -1;
    public int lastRound = -1;
    public int currentRound = -1;

    public LateFlatteningCondition(double min, int rounds) {
        super(min, rounds);
    }

    public LateFlatteningCondition(double min, int rounds, int lateRound) {
        super(min, rounds);
        this.lateRound = lateRound;
    }

    @Override
    public void mutate(Knn.ConditionMutation mutation) {
        currentRound = mutation.round;
    }

    @Override
    public boolean test(Object o) {
        boolean res = super.test(o) || (lastRound  >= lateRound);
        if(res)
            lastRound = currentRound;
        return res;
    }
}
