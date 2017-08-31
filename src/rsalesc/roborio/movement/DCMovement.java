package rsalesc.roborio.movement;

import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.movement.strategies.dc.*;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.DecayableStrategy;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class DCMovement extends TrueSurfing {
    private static final boolean SHOULD_SMOOTH_FLATTEN = true;
    private static final boolean SHOULD_FLATTEN = false;

    private static final double DISTANCE_THRESHOLD = 300;

    private static final Knn.HitCondition ADAPTIVE_LEAST = new Knn.HitLeastCondition(0.03, 0);

    public static Knn.ParametrizedCondition getSmootherCondition() {
        Knn.AndCondition and = new Knn.AndCondition().add(new Knn.OrCondition()
                .add(new LateFlatteningCondition(0.11, 3, DISTANCE_THRESHOLD, 15))
                .add(new LateFlatteningCondition(0.095, 5, DISTANCE_THRESHOLD, 15))
        );

        if(SHOULD_FLATTEN)
            and.add(new Knn.NotCondition(getFlatteningCondition()));

        return and;
    }

    public static Knn.ParametrizedCondition getFlatteningCondition() {
        return new Knn.OrCondition()
                .add(new LateFlatteningCondition(0.13, 3, DISTANCE_THRESHOLD,15))
                .add(new LateFlatteningCondition(0.11, 5, DISTANCE_THRESHOLD,15))
                .add(new LateFlatteningCondition(0.10, 8, DISTANCE_THRESHOLD,15))
                .add(new LateFlatteningCondition(0.09, 9, DISTANCE_THRESHOLD,15));
    }

    private static final DecayableStrategy ADAPTIVE_DECAY = new DecayableStrategy(0.8, 1.15)
            .setStrategy(new AdaptiveStrategy());

    private static final DecayableStrategy FLATTENING_DECAY = new DecayableStrategy(0.5, 1.15)
            .setStrategy(new FlatteningStrategy());

    private static final double ADAPTIVE_DECAY_DEPTH = 0.85;
    private static final double FLATTENER_DECAY_DEPTH = 0.85;

    private static final int FLATTENER_WEIGHT = 75;
    private static final int SMOOTHER_WEIGHT = 25;

    public DCMovement(BackAsFrontRobot robot) {
        super(robot, "washington-dc");
        this.setDodging(new DCDodging().setIdentifier("washington-dc-dodger"));
        this.build();
    }

    static class DCDodging extends DCGuessFactorDodging {
        @Override
        public KnnSet<GuessFactorRange> getNewKnnSet() {
            return getMonotonicKnnSet();
        }

        public KnnSet<GuessFactorRange> getOldReleaseKnnSet() {
            return new KnnSet<GuessFactorRange>()
                    .setDistanceWeighter(new Knn.GaussDistanceWeighter<>(1.0))
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(48)
                        .setRatio(0.25)
                        .setScanWeight(75)
                        .setStrategy(new DecayableStrategy(0.275, 1.15).setStrategy(new OldReleaseStrategy()))
                        .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(24)
                        .setRatio(0.25)
                        .setScanWeight(75)
                        .setStrategy(new DecayableStrategy(0.275, 1.15).setStrategy(new OldReleaseStrategy()))
                        .logsVirtual());
        }

        public KnnSet<GuessFactorRange> getDecayKnnSet() {
            return new KnnSet<GuessFactorRange>()
                    .setDistanceWeighter(new Knn.GaussDistanceWeighter<>(0.9))
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(30)
                        .setRatio(0.25)
                        .setScanWeight(1)
                        .setStrategy(new UnsegmentedStrategy())
                        .logsHit())

                    /* ADAPTIVE SMALL TREES */
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(20)
                        .setRatio(0.2)
                        .setScanWeight(30)
                        .setStrategy(new AdaptiveStrategy())
                        .setCondition(ADAPTIVE_LEAST)
                        .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setLimit(1)
                        .setK(1)
                        .setScanWeight(100)
                        .setCondition(ADAPTIVE_LEAST)
                        .setStrategy(new AdaptiveStrategy())
                        .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setLimit(1)
                        .setK(1)
                        .setScanWeight(100)
                        .setCondition(ADAPTIVE_LEAST)
                        .setStrategy(new AdaptiveStrategy())
                        .logsHit())

                    /* ADAPTIVE DECAY TREES */
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setK(8)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setDecayDepth(ADAPTIVE_DECAY_DEPTH)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setK(36)
                            .setRatio(0.33)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setDecayDepth(ADAPTIVE_DECAY_DEPTH)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setK(100)
                            .setRatio(0.5)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setDecayDepth(ADAPTIVE_DECAY_DEPTH)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())

                    /* FLATTENING TREES */
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(300)
                            .setK(25)
                            .setScanWeight(50)
                            .setRatio(0.08)
                            .setCondition(getFlatteningCondition())
                            .setDecayDepth(FLATTENER_DECAY_DEPTH)
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(2000)
                            .setK(50)
                            .setScanWeight(200)
                            .setRatio(0.07)
                            .setCondition(getFlatteningCondition())
                            .setDecayDepth(FLATTENER_DECAY_DEPTH)
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())

            ;
        }

        public KnnSet<GuessFactorRange> getMonotonicKnnSet() {
            KnnSet<GuessFactorRange> set = new KnnSet<GuessFactorRange>();

                set.add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setK(40)
                            .setRatio(0.2)
                            .setScanWeight(0.5)
                            .setStrategy(new AdaptiveStrategy())
                            .setDistanceWeighter(new Knn.GaussDistanceWeighter<>(1.0))
                            .logsHit())


                    /*
                    * ADAPTIVE MONOTONIC TREES
                     */
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
                            .setK(2)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(100)
                            .setK(3)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(1000)
                            .setK(3)
                            .setRatio(0.25)
                            .setScanWeight(100)
                            .setCondition(ADAPTIVE_LEAST)
                            .setStrategy(new AdaptiveStrategy())
                            .logsHit())
                    ;

                    if(SHOULD_SMOOTH_FLATTEN)
                    /*
                    *   SMOOTHING MONOTONIC TREES
                     */

                    set.add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(12)
                            .setK(2)
                            .setRatio(0.15)
                            .setScanWeight(40)
                            .setCondition(getSmootherCondition())
                            .setStrategy(new PreciseFlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(32)
                            .setK(3)
                            .setRatio(0.15)
                            .setScanWeight(40)
                            .setCondition(getSmootherCondition())
                            .setStrategy(new PreciseFlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(250)
                            .setK(6)
                            .setRatio(0.15)
                            .setScanWeight(40)
                            .setCondition(getSmootherCondition())
                            .setStrategy(new PreciseFlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(1000)
                            .setK(8)
                            .setRatio(0.15)
                            .setScanWeight(40)
                            .setCondition(getSmootherCondition())
                            .setStrategy(new PreciseFlatteningStrategy())
                            .logsBreak())
                    ;


                    /*
                    *   FLATTENING MONOTONIC TREES
                     */

                    if(SHOULD_FLATTEN)

                    set.add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(12)
                            .setK(2)
                            .setRatio(0.15)
                            .setScanWeight(75)
                            .setCondition(getFlatteningCondition())
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(32)
                            .setK(4)
                            .setRatio(0.15)
                            .setScanWeight(75)
                            .setCondition(getFlatteningCondition())
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(250)
                            .setK(6)
                            .setRatio(0.15)
                            .setScanWeight(75)
                            .setCondition(getFlatteningCondition())
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())
                    .add(new KnnTree<GuessFactorRange>()
                            .setMode(KnnTree.Mode.MANHATTAN)
                            .setLimit(1000)
                            .setK(6)
                            .setRatio(0.15)
                            .setScanWeight(75)
                            .setCondition(getFlatteningCondition())
                            .setStrategy(new FlatteningStrategy())
                            .logsBreak())

            ;

                    return set;
        }
    }
}
