package rsalesc.roborio.utils.nn;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public class SoftmaxStrategy extends MLPStrategy {
    @Override
    public double[] getActivation(double[] output) {
        double[] res = new double[output.length];
        double mx = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < output.length; i++) {
            if(output[i] > mx)
                mx = output[i];
        }

        double sum = 0;
        for(int i = 0; i < output.length; i++) {
            res[i] = R.exp(output[i] - mx);
            sum += res[i];
        }

        for(int i = 0; i < output.length; i++)
            res[i] /= sum;

        return res;
    }

    @Override
    public double getActivationDerivative(double a) {
        return a * (1.0 - a);
    }

    @Override
    public double getCost(double[][] a, double[][] y) {
        double acc = 0;
        for(int i = 0; i < y.length; i++) {
            for(int j = 0; j < y[0].length; j++) {
                acc += y[i][j] * Math.log(a[i][j]);
            }
        }

        return -acc / (y.length * y[0].length);
    }

    @Override
    public double[][] getLastLayerGradient(double[][] a, double[][] y) {
        double[][] res = new double[a.length][a[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                res[i][j] = a[i][j] - y[i][j];
            }
        }

        return res;
    }

    @Override
    public double[][] getNextLayerGradient(double[][] a, double[][] y) {
        double[][] res = new double[a.length][a[0].length];

        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                res[i][j] = getActivationDerivative(a[i][j]) * y[i][j];
            }
        }

        return res;
    }
}
