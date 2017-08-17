package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiShadowStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiShadowTargeting extends DCGuessFactorTargeting {
    public AntiShadowTargeting() {
        setIdentifier("shadow_targeting");
    }

    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
            .add(new KnnTree<GuessFactorRange>()
                .setLimit(10000)
                .setMode(KnnTree.Mode.MANHATTAN)
                .setK(52)
                .setRatio(0.15)
                .setStrategy(new AntiShadowStrategy()));
    }
}
