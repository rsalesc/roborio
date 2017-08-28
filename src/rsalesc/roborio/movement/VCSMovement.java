package rsalesc.roborio.movement;

import rsalesc.roborio.movement.strategies.vcs.*;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.stats.MultipleSegmentedBuffer;
import rsalesc.roborio.utils.stats.SegmentedBufferSet;
import rsalesc.roborio.utils.structures.Knn;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class VCSMovement extends TrueSurfing {
    private static final Knn.HitCondition ADAPTIVE_LEAST = new Knn.HitLeastCondition(0.04, 0);
    private static final Knn.HitCondition FLATTENER_LEAST = new LateFlatteningCondition(0.09, 1, 10);
    private static final Knn.HitCondition TICK_FLATTENER_LEAST = new LateFlatteningCondition(0.1, 1, 10);

    public VCSMovement(BackAsFrontRobot robot) {
        super(robot, "the-only-one");
        this.setDodging(new VCSDodging().setIdentifier("the-only-one-dodger"));
        this.build();
    }

    static class VCSDodging extends VCSGuessFactorDodging {
        @Override
        public SegmentedBufferSet getNewBufferSet() {
            return new SegmentedBufferSet()
                    .add(new MultipleSegmentedBuffer()
                        .setMultipleStrategy(new SlicingSimpleStrategy())
                        .weightsSegments()
                        .setRollingDepth(Double.POSITIVE_INFINITY)
                        .setWeight(1)
                        .logsHit())
                    .add(new MultipleSegmentedBuffer()
                        .setMultipleStrategy(new SlicingPatternMatcherStrategy())
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
                        .setWeight(25)
                        .setCondition(FLATTENER_LEAST)
                        .logsBreak())
//                    .add(new MultipleSegmentedBuffer()
//                        .setMultipleStrategy(new SlicingTickFlattenerStrategy())
//                        .weightsSegments()
//                        .setWeight(3.5)
//                        .setCondition(TICK_FLATTENER_LEAST)
//                        .logsVirtual())
                    ;
        }
    }
}
