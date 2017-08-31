package rsalesc.roborio.movement;

import rsalesc.roborio.utils.structures.Knn;

/**
 * Created by Roberto Sales on 28/08/17.
 */
public class LateFlatteningCondition extends Knn.HitLeastCondition {
    public int lateRound = -1;
    public int lastRound = -1;
    public int currentRound = -1;
    public double avgDistance;

    public LateFlatteningCondition(double min, int rounds, double avgDistance) {
        super(min, rounds);
        this.avgDistance = avgDistance;
    }

    public LateFlatteningCondition(double min, int rounds, double avgDistance, int lateRound) {
        super(min, rounds);
        this.lateRound = lateRound;
        this.avgDistance = avgDistance;
    }

    @Override
    public void mutate(Knn.ConditionMutation mutation) {
        currentRound = mutation.round;
    }

    @Override
    public boolean test(Object o) {
        if(!(o instanceof LateFlatteningCondition))
            throw new IllegalStateException();

        boolean res = (super.test(o) || (lastRound  >= lateRound));
        LateFlatteningCondition oo = (LateFlatteningCondition) o;
        res = res && oo.avgDistance > avgDistance;
        if(res)
            lastRound = currentRound;
        return res;
    }
}
