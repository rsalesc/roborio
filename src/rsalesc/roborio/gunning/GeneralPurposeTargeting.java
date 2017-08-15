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
                    .setLimit(6000)
                    .setMode(KnnTree.Mode.MANHATTAN)
                    .setK(60)
                    .setRatio(0.10)
                    .setStrategy(new GeneralPurposeStrategy()));
    }
}
