package rsalesc.roborio.gunning.targetings;

import rsalesc.roborio.gunning.DCGuessFactorTargeting;
import rsalesc.roborio.gunning.strategies.AntiRandom2Strategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiEverything2Targeting extends DCGuessFactorTargeting {
    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
                .setDistanceWeighter(new Knn.InverseDistanceWeighter<>(1.0))
                .add(new KnnTree<GuessFactorRange>()
                        .setMode(KnnTree.Mode.MANHATTAN)
                        .setK(225)
                        .setRatio(0.1)
                        .setStrategy(new AntiRandom2Strategy())
                        .logsHit()
                        .logsBreak()
                        .logsVirtual());
    }
}
