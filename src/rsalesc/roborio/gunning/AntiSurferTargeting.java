package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiSurferStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
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
                .setDistanceWeighter(new KnnSet.GaussDistanceWeighter<>(0.9))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(7500)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(3000)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(350)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<GuessFactorRange>()
                        .setLimit(125)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(5)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()));
    }
}
