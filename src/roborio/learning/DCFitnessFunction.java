package roborio.learning;

import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.CompositeGene;
import roborio.gunning.DCGuessFactorTargeting;
import roborio.learning.functions.NormFunction;
import roborio.utils.storage.NamedStorage;
import voidious.wavesim.WaveRunner;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class DCFitnessFunction extends FitnessFunction {
    @Override
    protected double evaluate(IChromosome chromo) {
        try {
            setup(chromo);
            return getScore(chromo);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    private double getScore(IChromosome chromo) {
        WaveRunner runner = new WaveRunner(DCClassifier.class, true);
        String[] op = new String[]{
                "jk.mega.DrussGT 1.3.8fTC",
                "jk.mini.CunobelinDC 0.1TC",
                "voidious.Dookious 1.573cTC",
                "abc.Shadow 3.66dTC",
                "wiki.BasicGFSurfer 1.02"
        };
        runner.simBattles(op, 10);

        NamedStorage.getInstance().clear();

        return runner.getTotals().getHitPercentage();
    }

    public static double[][] getWeights(IChromosome chromo) {
        double[][] weights = new double[2][];
        for(int i = 0; i < 2; i++) {
            weights[i] = new double[DCWaveSim.DIMENSIONS];
            CompositeGene dimensionGene = (CompositeGene) chromo.getGene(i);

            for(int j = 0; j < DCWaveSim.DIMENSIONS; j++) {
                Gene gene = dimensionGene.geneAt(j);
                weights[i][j] = (Double) gene.getAllele();
            }
        }
        return weights;
    }

    public static Class<? extends NormFunction>[] getFunctions(IChromosome chromo) {
        Class<? extends NormFunction>[] fns = new Class[DCWaveSim.DIMENSIONS];
        for(int i = 0; i < DCWaveSim.DIMENSIONS; i++) {
            fns[i] = DCGuessFactorTargeting.NORM_FUNCTIONS[i].getClass();
        }

        return fns;
    }

    public static double[][] getParams(IChromosome chromo) {
        NormFunction[] funcs = DCGuessFactorTargeting.NORM_FUNCTIONS;
        double[][] fns = new double[DCWaveSim.DIMENSIONS][];
        CompositeGene dimensionGene = (CompositeGene) chromo.getGene(2);
        int ptr = 0;

        for(int i = 0; i < DCWaveSim.DIMENSIONS; i++) {
            if(funcs[i].arity() > 0) {
                double[] cur = new double[funcs[i].arity()];
                for(int j = 0; j < funcs[i].arity(); j++) {
                    cur[j] = (Double) dimensionGene.geneAt(ptr).getAllele();
                    ptr++;
                }

                fns[i] = cur;
            } else {
                fns[i] = new double[0];
            }
        }

        return fns;
    }

    public static void setup(IChromosome chromo) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        double[][] weights = getWeights(chromo);
        DCGuessFactorTargeting.TREE_WEIGHTS = weights[0];
        DCGuessFactorTargeting.FAST_TREE_WEIGHTS = weights[1];

        Class<? extends NormFunction>[] fns = getFunctions(chromo);
        double[][] params = getParams(chromo);

        DCGuessFactorTargeting.NORM_FUNCTIONS = new NormFunction[DCWaveSim.DIMENSIONS];
        for(int i = 0; i < fns.length; i++) {
            DCGuessFactorTargeting.NORM_FUNCTIONS[i] =
                    fns[i].getDeclaredConstructor(double[].class).newInstance(params[i]);
        }

        DCGuessFactorTargeting.EUCLIDEAN_TREE = (Boolean) chromo.getGene(3).getAllele();
    }
}
