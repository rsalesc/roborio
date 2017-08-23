package rsalesc.roborio.utils.nn;

import rsalesc.roborio.utils.MatrixUtils;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public class MLP {
    private boolean         built = false;

    private int[]           layersSizes;
    private double[][][]    weights;
    private double[][]      bias;
    private double[][][]    act;
    private int             batchSize;
    private int             outputSize;

    private MLPStrategy     strategy;
    private MLPRegularization regularization;

    public MLP(int inputSize, int[] hiddenSizes, int outputSize) {
        layersSizes = new int[hiddenSizes.length + 2];
        for(int i = 0; i < hiddenSizes.length; i++)
            layersSizes[i+1] = hiddenSizes[i];
        layersSizes[0] = inputSize;
        layersSizes[hiddenSizes.length + 1] = outputSize;
        this.outputSize = outputSize;
    }

    public MLP(int inputSize, int hiddenSize, int outputSize) {
        this(inputSize, new int[]{hiddenSize}, outputSize);
    }

    private void setupMatrices() {
        weights = new double[layersSizes.length - 1][][];
        bias = new double[layersSizes.length - 1][];
        act = new double[layersSizes.length][][];
        for(int i = 0; i < layersSizes.length - 1; i++) {
            weights[i] = new double[layersSizes[i+1]][layersSizes[i]];
            bias[i] = new double[layersSizes[i+1]];
        }
    }

    public MLP build() {
        setupMatrices();
        weights[0][0][0] = 0.01;

        built = true;
        return this;
    }

    public MLP buildRandomly() {
        build();
        for(int l = 0; l < layersSizes.length - 1; l++) {
            int neurons = layersSizes[l + 1];
            for(int i = 0; i < neurons; i++) {
                for(int j = 0; j < layersSizes[l]; j++) {
                    weights[l][i][j] = Math.random();
                }

                bias[l][i] = Math.random();
            }
        }

        return this;
    }

    public MLP setStrategy(MLPStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public MLPStrategy getStrategy() {
        return strategy;
    }

    public int getInputSize() {
        return layersSizes[0];
    }

    public int getOutputSize() {
        return layersSizes[layersSizes.length - 1];
    }

    public double train(double[][] batch, double[][] expected, double rate) {
        feed(batch);
        propagate(expected, rate);
        return getCost(expected);
    }

    public double train(double[] input, double[] output, double rate) {
        feed(input);
        propagate(output, rate);
        return getCost(output);
    }

    public double[][] feed(double[][] batch) {
        batchSize = batch.length;
        if(batchSize == 0)
            return null;

        double[][] cur = transpose(batch);
        act[0] = cur;

        for(int i = 0; i < layersSizes.length - 1; i++) {
            double[][] next = multiply(weights[i], cur);
            for(int j = 0; j < batchSize; j++) {
                for(int k = 0; k < layersSizes[i + 1]; k++) {
                    next[k][j] += bias[i][k];
                }

                double[] a = strategy.getActivation(MatrixUtils.getColumn(next, j));
                for(int k = 0; k < layersSizes[i + 1]; k++)
                    next[k][j] = a[k];
            }

            cur = next;
            act[i+1] = cur;
        }

        return transpose(cur);
    }

    public void propagate(double[] expected, double rate) {
        propagate(new double[][]{expected}, rate);
    }

    public void propagate(double[][] expected, double rate) {
        if(act[0] == null || act[0][0] == null || expected.length != act[0][0].length)
            throw new IllegalStateException("propagation must have the same size of feeded data");

        double[][] output = act[layersSizes.length - 1];
        backPropagate(layersSizes.length - 2, transpose(expected), rate, true);
    }

    private void backPropagate(int hiddenLayer, double[][] prop, double rate, boolean lastLayer) {
        double[][] error = lastLayer ? strategy.getLastLayerGradient(act[hiddenLayer + 1], prop) : prop;

        if(hiddenLayer > 0)
            backPropagate(hiddenLayer - 1,
                    strategy.getNextLayerGradient(act[hiddenLayer], multiply(transpose(weights[hiddenLayer]), error)),
                    rate, false);

        double[][] prevActivation = act[hiddenLayer];

        double[][] weightDelta = multiply(error, transpose(prevActivation)); // weird
        if(regularization != null) {
            add(weightDelta, regularization.getDerivative(weights[hiddenLayer]), batchSize);
        }

        double[] biasDelta = meanColumns(error); // faz sentido

        add(weights[hiddenLayer], weightDelta, -rate / batchSize);
        add(bias[hiddenLayer], biasDelta, -rate);
    }

    public double[] feed(double[] input) {
        return feed(new double[][]{input})[0];
    }

    private double[][] transpose(double[][] a) {
        double[][] b = new double[a[0].length][a.length];
        for(int i = 0;  i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                b[j][i] = a[i][j];
            }
        }

        return b;
    }

    private double[][] multiply(double[][] a, double[][] b) {
        if(a[0].length != b.length)
            throw new IllegalStateException("multiplication of non adequate matrices");

        int rows = a.length;
        int mid = b.length;
        int cols = b[0].length;
        double[][] res = new double[rows][cols];

        for(int i = 0; i < rows; i++) {
            for(int k = 0; k < mid; k++) {
                for(int j = 0; j < cols; j++) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        return res;
    }

    private double[] meanColumns(double[][] a) {
        double[] res = new double[a.length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                res[i] += a[i][j];
            }
            res[i] /= a[0].length;
        }

        return res;
    }

    private void add(double[][] a, double[][] b, double rate) {
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                a[i][j] += b[i][j] * rate;
            }
        }
    }

    private void add(double[] a, double[] b, double rate) {
        for(int i = 0; i < a.length; i++) {
            a[i] += b[i] * rate;
        }
    }

    public double getCost(double[][] expected) {
        double[][] y = transpose(expected);
        double[][] a = act[layersSizes.length - 1];

        double res = strategy.getCost(a, y);

        return res;
    }

    public double getCost(double[] expected) {
        return getCost(new double[][]{expected});
    }

    public MLP setRegularization(MLPRegularization regularization) {
        this.regularization = regularization.setOutputSize(outputSize);
        return this;
    }
}
