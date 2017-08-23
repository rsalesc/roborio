package rsalesc.roborio.utils.nn;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public class LogisticStrategy extends MLPStrategy {
    @Override
    public double[] getActivation(double[] output) {
        double[] res = new double[output.length];
        for(int i = 0; i < res.length; i++)
            res[i] = 1.0 / (1.0 + R.exp(-output[i]));
        return res;
    }

    @Override
    public double getActivationDerivative(double a) {
        return a * (1.0 - a);
    }

    public double getCostGradient(double a, double y) {
        double res = (a - y) / (a * (1.0 - a));
        if(Double.isNaN(res))
            return 0.0;
        return res;
    }

    @Override
    public double getCost(double[][] a, double[][] y) {
        double acc = 0;
        for(int i = 0; i < y.length; i++) {
            for(int j = 0; j < y[0].length; j++) {
                acc += y[i][j] * Math.log(a[i][j]) + (1.0 - y[i][j]) * Math.log(1.0 - a[i][j]);
            }
        }

        return -acc / (y.length * y[0].length);
    }

    @Override
    public double[][] getLastLayerGradient(double[][] a, double[][] y) {
        double[][] res = new double[a.length][a[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                res[i][j] = getCostGradient(a[i][j], y[i][j]) * getActivationDerivative(a[i][j]);
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
