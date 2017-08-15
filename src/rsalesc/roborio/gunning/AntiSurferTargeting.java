package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiSurferStrategy;
import rsalesc.roborio.structures.KnnSet;
import rsalesc.roborio.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiSurferTargeting extends DCGuessFactorTargeting {
    public AntiSurferTargeting() {
        setIdentifier("surf_targeting");
    }

    @Override
    public KnnSet<Double> getKnnSet() {
        return new KnnSet<Double>()
                .add(new KnnTree<Double>()
                        .setLimit(7500)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(6)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<Double>()
                        .setLimit(3000)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(6)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<Double>()
                        .setLimit(350)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(6)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()))
                .add(new KnnTree<Double>()
                        .setLimit(125)
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(6)
                        .setRatio(0.15)
                        .setStrategy(new AntiSurferStrategy()));
    }
}
