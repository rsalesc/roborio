package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiSurferStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 16/08/17.
 */
public class AntiSurferPlusTargeting extends DCGuessFactorTargeting {
    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
                .setDistanceWeighter(new KnnSet.GaussDistanceWeighter<>(0.9))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(7500)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(4)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(2000)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(4)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(350)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(4)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(125)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(4)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(32)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(2)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy())
                        .logsBreak())

                // hit negative
                .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setLimit(4)
                        .setK(1)
                        .setRatio(1)
                        .setScanWeight(-0.5) // -1 was pretty good actually
                        .setStrategy(new AntiSurferStrategy())
                        .logsHit())

                ;
    }
}
