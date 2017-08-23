package rsalesc.roborio.utils.nn;

import org.junit.jupiter.api.Test;

/**
 * Created by Roberto Sales on 17/08/17.
 */
class MLPTest {
    @Test
    public void test() {
        final int iterations = 10000;
        final int tests = 64;
        final int bitLength = 3;
        final int batchSize = 8;

        MLP net = new MLP(2 * bitLength, new int[]{2*bitLength}, bitLength)
                .setStrategy(new LogisticStrategy())
                .setRegularization(new L2Regularization(0.05))
                .buildRandomly();

        int[][] input = new int[tests][2 * bitLength];
        int[][] output = new int[tests][bitLength];

        double[][] inputFloat = new double[tests][2 * bitLength];
        double[][] outputFloat = new double[tests][bitLength];

        for(int i = 0; i < tests; i++) {
            for (int j = 0; j < 2 * bitLength; j++) {
                input[i][j] = (int) (Math.random() * 1.99999999);
                output[i][j / 2] ^= input[i][j];
            }

            for(int j = 0; j < 2 * bitLength; j++)
                inputFloat[i][j] = input[i][j];

            for(int j = 0; j < bitLength; j++)
                outputFloat[i][j] = output[i][j];
        }

        for(int i = 0; i < iterations; i++) {
            double loss = 0;

            for(int j = 0; j < tests; j += batchSize) {
                int len = Math.min(batchSize, tests - j);
                double[][] inputBatch = new double[len][2 * bitLength];
                double[][] outputBatch = new double[len][bitLength];

                for(int k = 0; k < len; k++) {
                    for(int l = 0; l < 2 * bitLength; l++) {
                        inputBatch[k][l] = inputFloat[j + k][l];
                    }
                }

                for(int k = 0; k < len; k++) {
                    for(int l = 0; l < bitLength; l++) {
                        outputBatch[k][l] = outputFloat[j + k][l];
                    }
                }

                net.train(inputBatch, outputBatch, 0.25);
                double old = net.getCost(outputBatch);

                loss += old;
            }

            loss /= (tests + batchSize - 1) / batchSize;
            System.out.println("loss: " + loss);
        }

        for(int i = 0; i < tests; i++) {
            double[] out = net.feed(inputFloat[i]);

            System.out.print("Found: ");
            for(int j = 0; j < out.length; j++) {
                System.out.print((int)(out[j] * 1.9999999999) + " ");
            }

            System.out.print("Expected: ");
            for(int j = 0; j < out.length; j++) {
                System.out.print(output[i][j] + " ");
            }

            System.out.println();
        }
    }
}