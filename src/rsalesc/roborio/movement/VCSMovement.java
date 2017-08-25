package rsalesc.roborio.movement;

import rsalesc.roborio.movement.strategies.SlicingAdaptiveStrategy;
import rsalesc.roborio.movement.strategies.SlicingFlattenerStrategy;
import rsalesc.roborio.movement.strategies.SlicingSimpleStrategy;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.stats.MultipleSegmentedBuffer;
import rsalesc.roborio.utils.stats.SegmentedBufferSet;
import rsalesc.roborio.utils.structures.Knn;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class VCSMovement extends TrueSurfing {
    private static final Knn.HitCondition ADAPTIVE_LEAST = new Knn.HitLeastCondition(0.03, 0);
    private static final Knn.HitCondition FLATTENER_LEAST = new Knn.HitLeastCondition(0.07, 1);

    public VCSMovement(BackAsFrontRobot robot) {
        super(robot, "the-only-one");
        this.setDodging(new VCSDodging().setIdentifier("the-only-one-dodger"));
        this.build();
    }

    static class VCSDodging extends VCSGuessFactorDodging {
        @Override
        public SegmentedBufferSet getBufferSet() {
            return new SegmentedBufferSet()
                    .add(new MultipleSegmentedBuffer()
                        .setMultipleStrategy(new SlicingSimpleStrategy())
                        .weightsSegments()
                        .setRollingDepth(Double.POSITIVE_INFINITY)
                        .setWeight(1)
                        .logsHit())
                    .add(new MultipleSegmentedBuffer()
                        .setMultipleStrategy(new SlicingAdaptiveStrategy())
                        .weightsSegments()
                        .setWeight(100)
                        .setCondition(ADAPTIVE_LEAST)
                        .logsHit())
                    .add(new MultipleSegmentedBuffer()
                        .setMultipleStrategy(new SlicingFlattenerStrategy())
                        .weightsSegments()
                        .setWeight(40)
                        .setCondition(FLATTENER_LEAST)
                        .logsBreak())
                    ;
        }
    }
}
