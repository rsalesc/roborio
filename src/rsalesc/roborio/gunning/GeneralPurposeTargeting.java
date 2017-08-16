package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.GeneralPurposeStrategy;
import rsalesc.roborio.structures.KnnSet;
import rsalesc.roborio.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class GeneralPurposeTargeting extends DCGuessFactorTargeting {
    @Override
    public KnnSet<Double> getKnnSet() {
        return new KnnSet<Double>()
                .add(new KnnTree<Double>()
                    .setMode(KnnTree.Mode.MANHATTAN)
                    .setK(72)
                    .setRatio(0.20)
                    .setStrategy(new GeneralPurposeStrategy()));
    }
}
