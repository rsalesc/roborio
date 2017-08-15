package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiShadowStrategy;
import rsalesc.roborio.structures.KnnSet;
import rsalesc.roborio.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiShadowTargeting extends DCGuessFactorTargeting {
    public AntiShadowTargeting() {
        setIdentifier("shadow_targeting");
    }

    @Override
    public KnnSet<Double> getKnnSet() {
        return new KnnSet<Double>()
            .add(new KnnTree<Double>()
                .setLimit(10000)
                .setMode(KnnTree.Mode.MANHATTAN)
                .setK(52)
                .setRatio(0.15)
                .setStrategy(new AntiShadowStrategy()));
    }
}
