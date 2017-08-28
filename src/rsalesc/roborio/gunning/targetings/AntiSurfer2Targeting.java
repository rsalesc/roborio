package rsalesc.roborio.gunning.targetings;

import rsalesc.roborio.gunning.DCGuessFactorTargeting;
import rsalesc.roborio.gunning.strategies.AntiSurferStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 16/08/17.
 */
public class AntiSurfer2Targeting extends DCGuessFactorTargeting {
    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
//                .setDistanceWeighter(new KnnSet.GaussDistanceWeighter<>(0.9))
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
                        .logsBreak());
    }
}
