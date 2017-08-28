package rsalesc.roborio.gunning.targetings;

import rsalesc.roborio.gunning.DCGuessFactorTargeting;
import rsalesc.roborio.gunning.strategies.AntiSurferStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiSurferTargeting extends DCGuessFactorTargeting {
    public AntiSurferTargeting() {
        setIdentifier("surf_targeting");
    }

    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
                .setDistanceWeighter(new Knn.GaussDistanceWeighter<>(0.9))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(7500)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(3000)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(350)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(125)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(25)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(3)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())

                // hits
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(4)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(1)
                        .setScanWeight(1.5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsHit())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(8)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(2)
                        .setScanWeight(1.5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsHit());
    }
}
