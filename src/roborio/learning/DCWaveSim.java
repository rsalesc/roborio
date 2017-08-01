package roborio.learning;

import org.jgap.*;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import voidious.wavesim.WaveRunner;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public class DCWaveSim {
    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 100;

    public static void main(String[] args) throws InvalidConfigurationException {
        WaveRunner.BASEDIR = "/home/rsalesc/robocode/wavesim/data";
        WaveRunner.initializeThreads();
        doGA();
        WaveRunner.shutdownThreads();
    }

    private static void doGA() throws InvalidConfigurationException {
        Configuration conf = new DefaultConfiguration();
        conf.setFitnessFunction(new DCFitnessFunction());

        Gene[] genes = new Gene[28];

        for(int i = 0; i < 20; i++) {
            genes[i] = new DoubleGene(conf, 0, 4);
        }

        genes[20] = new DoubleGene(conf, 0,1);
        genes[21] = new DoubleGene(conf, 0, 4);
        genes[22] = new DoubleGene(conf, 0.85, 1.3);
        genes[23] = new DoubleGene(conf, 0, 4);
        genes[24] = new DoubleGene(conf, 1.0, 1.2);

        // percent
        genes[25] = new DoubleGene(conf, 0.0, 1.0);

        genes[26] = new IntegerGene(conf, 8, 100);
        genes[27] = new IntegerGene(conf, 8, 50);

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

            dumpWeights(bestSoFar);

            System.out.println("Best on generation " + i + ": " + value);
        }

        System.out.println();
        System.out.println("Finished!");
        dumpWeights(bestSoFar);
    }

    private static void dumpWeights(IChromosome chromo) {
        double[][] res = DCFitnessFunction.getWeights(chromo);
        for(int ii = 0; ii < 3; ii++) {
            System.out.print("new double[]{");
            for(int j = 0; j < res[ii].length; j++) {
                if(j > 0) System.out.print(", ");
                System.out.printf("%.8f", (float) res[ii][j]);
            }
            System.out.println("};");
        }

        double[] percents = DCFitnessFunction.getPercents(chromo);
        System.out.print("new double[]{");
        for(int i = 0; i < 2; i++) {
            if(i > 0) System.out.print(", ");
            System.out.printf("%.8f", (float) percents[i]);
        }
        System.out.println("};");

        int[] K = DCFitnessFunction.getK(chromo);
        System.out.print("new int[]{");
        for(int i = 0; i < 2; i++) {
            if(i > 0) System.out.print(", ");
            System.out.printf("%.8f", (float) K[i]);
        }
        System.out.println("};");
    }
}
