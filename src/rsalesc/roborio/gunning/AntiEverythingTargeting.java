package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiRandomStrategy;
import rsalesc.roborio.gunning.utils.GuessFactorRange;
import rsalesc.roborio.utils.structures.KnnSet;
import rsalesc.roborio.utils.structures.KnnTree;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class AntiEverythingTargeting extends DCGuessFactorTargeting {
    @Override
    public KnnSet<GuessFactorRange> getKnnSet() {
        return new KnnSet<GuessFactorRange>()
//                .setDistanceWeighter(new KnnSet.GaussDistanceWeighter<>(0.9))
                .add(new KnnTree<GuessFactorRange>()
                    .setMode(KnnTree.Mode.MANHATTAN)
                    .setK(100)
                    .setRatio(0.25)
                    .setStrategy(new AntiRandomStrategy())
                    .logsHit()
                    .logsBreak())
                .add(new KnnTree<GuessFactorRange>()
                    .setMode(KnnTree.Mode.MANHATTAN)
                    .setK(50)
                    .setRatio(0.15)
                    .setStrategy(new AntiRandomStrategy())
                    .logsVirtual());
    }
}
