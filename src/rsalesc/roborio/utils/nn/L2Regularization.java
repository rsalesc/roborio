package rsalesc.roborio.utils.nn;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public class L2Regularization extends MLPRegularization {
    private double lambda;

    public L2Regularization(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double getValue(double[][][] weights) {
        double res = 0;
        for(int i = 0; i < weights.length; i++) {
            for(int j = 0; j < weights[i].length; j++) {
                for(int k = 0; k < weights[i][j].length; k++) {
                    res += sqr(weights[i][j][k]);
                }
            }
        }

        return res * lambda / 2;
    }

    @Override
    public double[][] getDerivative(double[][] w) {
        double[][] res = new double[w.length][w[0].length];

        for(int i = 0; i < w.length; i++) {
            for(int j = 0; j < w[0].length; j++) {
                res[i][j] = w[i][j] * lambda / outputSize;
            }
        }

        return res;
    }

    public double sqr(double x) {
        return x*x;
    }
}
