package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.movement.strategies.AdaptiveStrategy;
import rsalesc.roborio.movement.strategies.FlatteningStrategy;
import rsalesc.roborio.movement.strategies.UnsegmentedStrategy;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.DecayableStrategy;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class DCMovement extends TrueSurfing {
    private static final Knn.HitCondition ADAPTIVE_LEAST = new Knn.HitLeastCondition(0.02, 0);
    private static final Knn.HitCondition FLATTENER_LEAST = new Knn.HitLeastCondition(0.09, 1);
    private static final Knn.HitCondition SMOOTH_LEAST = new Knn.HitLeastCondition(0.06, 1);

    private static final DecayableStrategy ADAPTIVE_DECAY = new DecayableStrategy(0.8, 1.15)
            .setStrategy(new AdaptiveStrategy());

    private static final DecayableStrategy FLATTENING_DECAY = new DecayableStrategy(0.5, 1.15)
            .setStrategy(new FlatteningStrategy());

    public DCMovement(BackAsFrontRobot robot) {
        super(robot, "the-only-one");
        this.setDodging(new DCDodging());
        this.build();
    }

    static class DCDodging extends DCGuessFactorDodging {
        @Override
        public KnnSet<GuessFactorRange> getKnnSet() {
            return new KnnSet<GuessFactorRange>()
                    .setDistanceWeighter(new KnnSet.GaussDistanceWeighter<>(0.9))
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setK(24)
                            .setRatio(0.2)
                            .setScanWeight(1)
                            .setStrategy(new UnsegmentedStrategy())
                            .logsHit())


                    // adaptive breaking (needs a condition?)
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(1)
                            .setK(1)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(8)
                            .setK(2)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(32)
                            .setK(4)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(100)
                            .setK(8)
                            .setRatio(0.25)
                            .setScanWeight(50)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(1000)
                            .setK(32)
                            .setRatio(0.25)
                            .setScanWeight(10)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())

                    // really need it over here
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(32)
                            .setK(4)
                            .setRatio(0.15)
                            .setScanWeight(10)
                            .setCondition(FLATTENER_LEAST)
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(100)
                            .setK(12)
                            .setRatio(0.15)
                            .setScanWeight(20)
                            .setCondition(FLATTENER_LEAST)
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak());
        }
    }
}
