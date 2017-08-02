package roborio.learning.functions;

/**
 * Created by Roberto Sales on 01/08/17.
 */
public class InverseFunction extends NormFunction {
    public InverseFunction(double[] p) {
        super(p);
    }

    @Override
    public double evaluate(double x) {
        return 1.0 / (Math.abs(p[0]*x) + 1);
    }

    @Override
    public int arity() {
        return 1;
    }
}
