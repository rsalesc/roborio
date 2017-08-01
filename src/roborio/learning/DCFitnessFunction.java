package roborio.learning;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import roborio.gunning.DCGuessFactorTargeting;
import voidious.wavesim.WaveRunner;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class DCFitnessFunction extends FitnessFunction {
    @Override
    protected double evaluate(IChromosome chromo) {
        return getHitPercentage(chromo);
    }

    public static double[][] getWeights(IChromosome chromo) {
        double[][] weights = new double[3][];
        weights[0] = new double[10];
        weights[1] = new double[10];
        weights[2] = new double[5];

        for(int i = 0; i < 25; i++) {
            weights[i/10][i%10] = (Double) chromo.getGene(i).getAllele();
        }

        return weights;
    }

    public static double[] getPercents(IChromosome chromo) {
        double percent = (Double) chromo.getGene(25).getAllele();
        return new double[]{percent, 1.0 - percent};
    };

    public static int[] getK(IChromosome chromo) {
        return new int[]{
                (Integer) chromo.getGene(26).getAllele(),
                (Integer) chromo.getGene(27).getAllele()
        };
    }

    public static double getHitPercentage(IChromosome chromo) {
        DCGuessFactorTargeting.TREE_WEIGHTS = new double[10];
        DCGuessFactorTargeting.FAST_TREE_WEIGHTS = new double[10];
        DCGuessFactorTargeting.NORMALIZING_PARAMS = new double[5];

        for(int i = 0; i < 10; i++)
            DCGuessFactorTargeting.TREE_WEIGHTS[i] = (Double) chromo.getGene(i).getAllele();
        for(int i = 10; i < 20; i++)
            DCGuessFactorTargeting.FAST_TREE_WEIGHTS[i-10] = (Double) chromo.getGene(i).getAllele();
        for(int i = 20; i < 25; i++)
            DCGuessFactorTargeting.NORMALIZING_PARAMS[i-20] = (Double) chromo.getGene(i).getAllele();

        double percent = (Double) chromo.getGene(25).getAllele();
        DCGuessFactorTargeting.STATS_WEIGHTS = new double[]{percent, 1.0 - percent};
        DCGuessFactorTargeting.STATS_K = new int[]{
                (Integer) chromo.getGene(26).getAllele(),
                (Integer) chromo.getGene(27).getAllele()
        };

        WaveRunner runner = new WaveRunner(DCClassifier.class, true);
        runner.simBattles(new String[]{
                "cx.mini.Cigaret 1.31",
                "BIRL.Monstro 1.0",
                "wiki.BasicGFSurfer 1.02",
                "abc.Shadow 3.84"
        }, 10);

        return runner.getTotals().getHitPercentage();
    }
}
