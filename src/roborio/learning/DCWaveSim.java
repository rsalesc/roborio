package roborio.learning;

import org.jgap.*;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.CompositeGene;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import roborio.gunning.DCGuessFactorTargeting;
import roborio.learning.functions.NormFunction;
import roborio.learning.functions.PowerFunction;
import voidious.wavesim.WaveRunner;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class DCWaveSim {
    private static final int POPULATION_SIZE = 500;
    private static final int GENERATIONS = 50;

    public static final int DIMENSIONS = 12;

    public static void main(String[] args) throws InvalidConfigurationException {
        WaveRunner.BASEDIR = "/home/rsalesc/robocode/wavesim/data";
        WaveRunner.initializeThreads();
//        doGenetics();
        bulletTest();
        WaveRunner.shutdownThreads();
    }

    private static void bulletTest() {
        WaveRunner runner = new WaveRunner(DCClassifier.class, true);
        String[] op = new String[]{
//                "jk.mega.DrussGT 1.3.8fTC",
                "jk.mini.CunobelinDC 0.1TC",
//                "voidious.Dookious 1.573cTC",
//                "abc.Shadow 3.66dTC",
//                "wiki.BasicGFSurfer 1.02"
        };
        runner.simBattles(op, 10);
        System.out.println(runner.getTotals().damageDone / (35*op.length));
    }

    private static void doGenetics() throws InvalidConfigurationException {
        Configuration conf = new DefaultConfiguration();
        conf.setFitnessFunction(new DCFitnessFunction());

        Gene[] genes = new Gene[4];

        for(int i = 0; i < 2; i++) {
            CompositeGene dimensionGene = new CompositeGene(conf);
            for(int j = 0; j < DIMENSIONS; j++) {
                dimensionGene.addGene(new DoubleGene(conf, 0, 2.5));
            }
            genes[i] = dimensionGene;
        }

        CompositeGene comp = new CompositeGene(conf);
        for(int j = 0; j < DIMENSIONS; j++) {
            NormFunction func = DCGuessFactorTargeting.NORM_FUNCTIONS[j];
            if(func.arity() > 0) {
                comp.addGene(new DoubleGene(conf, 0, 10));
                if(func.getClass() == PowerFunction.class) {
                    comp.addGene(new DoubleGene(conf, 1.0, 1.2));
                }

            }
        }

        genes[2] = comp;
        genes[3] = new BooleanGene(conf);

        Chromosome sample = new Chromosome(conf, genes);
        conf.setSampleChromosome(sample);
        conf.setPopulationSize(POPULATION_SIZE);

        Genotype population = Genotype.randomInitialGenotype(conf);

        IChromosome bestSoFar = null;
        double value = -1;

        for(int i = 0; i < GENERATIONS; i++) {
            population.evolve();
            IChromosome cur = population.getFittestChromosome();
            double curValue = cur.getFitnessValueDirectly();
            if(curValue > value) {
                value = curValue;
                bestSoFar = cur;
            }

            dump(bestSoFar);

            System.out.println("Best on generation " + i + ": " + value);
        }

        System.out.println();
        System.out.println("Finished!");
        dump(bestSoFar);
    }

    private static void dump(IChromosome chromo) {
        double[][] weights = DCFitnessFunction.getWeights(chromo);
        for(int i = 0; i < 2; i++) {
            dump(weights[i]);
        }

        dumpFunctions(DCFitnessFunction.getFunctions(chromo), DCFitnessFunction.getParams(chromo));
        if((Boolean) chromo.getGene(3).getAllele()) {
            System.out.println("Using EUCLIDEAN!");
        } else {
            System.out.println("Using Manhattan :(");
        }
    }

    private static void dumpArray(double[] a) {
        System.out.print("new double[]{");
        for(int i = 0; i < a.length; i++) {
            if(i > 0) System.out.print(", ");
            System.out.printf("%.8f", a[i]);
        }
        System.out.print("}");
    }

    private static void dump(double[] a) {
        dumpArray(a);
        System.out.println("");
    }

    private static void dumpFunctions(Class[] fns, double[][] params) {
        System.out.println("new NormFunction[]{");
        for(int i = 0; i < fns.length; i++) {
            if(i > 0)
                System.out.println(",");
            System.out.print("\t");
            System.out.print("new " + fns[i].getSimpleName() + "(");
            dumpArray(params[i]);
            System.out.print(")");
        }

        System.out.println("");
        System.out.println("};");
    }
}
