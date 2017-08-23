package rsalesc.roborio.utils.nn;

/**
 * Created by Roberto Sales on 17/08/17.
 */
public abstract class MLPStrategy {
    public abstract double[] getActivation(double[] output);
    public abstract double getActivationDerivative(double a);
//    public abstract double getCostGradient(double a, double y);
    public abstract double getCost(double[][] a, double[][] y);

    public abstract double[][] getLastLayerGradient(double[][] a, double[][] y);
    public abstract double[][] getNextLayerGradient(double[][] a, double[][] y);
}
