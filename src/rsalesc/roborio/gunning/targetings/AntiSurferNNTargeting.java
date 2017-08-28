package rsalesc.roborio.gunning.targetings;

import rsalesc.roborio.gunning.NNGuessFactorTargeting;
import rsalesc.roborio.gunning.strategies.AntiSurferNNStrategy;
import rsalesc.roborio.utils.nn.GuessFactorNetwork;
import rsalesc.roborio.utils.nn.SoftmaxStrategy;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public class AntiSurferNNTargeting extends NNGuessFactorTargeting {
    @Override
    protected GuessFactorNetwork getNetwork() {
        GuessFactorNetwork nn = new GuessFactorNetwork()
                .setSlicingStrategy(new AntiSurferNNStrategy())
                .setNetworkStrategy(new SoftmaxStrategy())
                .build();

        return nn;
    }
}
