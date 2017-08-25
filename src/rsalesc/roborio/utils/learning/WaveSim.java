package rsalesc.roborio.utils.learning;

import org.jgap.InvalidConfigurationException;
import voidious.wavesim.WaveRunner;

/**
 * Created by Roberto Sales on 24/08/17.
 */
public class WaveSim {
    private static final int POPULATION_SIZE = 500;
    private static final int GENERATIONS = 50;

    public static final int DIMENSIONS = 12;

    public static void main(String[] args) throws InvalidConfigurationException {
        WaveRunner.BASEDIR = "/home/rsalesc/robocode/robots/voidious/TripHammer.data";
        WaveRunner.initializeThreads();
        bulletTest();
        WaveRunner.shutdownThreads();
    }

    private static void bulletTest() {
        WaveRunner runner = new WaveRunner(TargetingClassifier.class, true);
        String[] op = new String[]{
                "cx.mini.Cigaret 1.31",
                "dummy.micro.Sparrow 2.5",
                "gh.GrubbmGrb 1.2.4",
                "kawigi.sbf.FloodMini 1.4",
                "pe.SandboxDT 2.71m"
        };
        runner.simBattles(op, 10);
        System.out.println(
            runner.getTotals().getHitPercentage());
    }
}
