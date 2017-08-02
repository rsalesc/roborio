package roborio.learning.functions;

/**
 * Created by Roberto Sales on 01/08/17.
 */
public abstract class NormFunction {
    protected double[] p;
    public NormFunction(double[] p) {
        this.p = p;
    }

    public abstract double evaluate(double x);
    public abstract int arity();
}
