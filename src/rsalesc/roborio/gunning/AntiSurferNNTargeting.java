package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.strategies.AntiSurferNNStrategy;
import rsalesc.roborio.utils.nn.GuessFactorNetwork;
import rsalesc.roborio.utils.nn.LogisticStrategy;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public class AntiSurferNNTargeting extends NNGuessFactorTargeting {
    @Override
    protected GuessFactorNetwork getNetwork() {
        GuessFactorNetwork nn = new GuessFactorNetwork()
                .setSlicingStrategy(new AntiSurferNNStrategy())
                .setNetworkStrategy(new LogisticStrategy())
                .build();

        return nn;
    }
}
