package roborio.learning.functions;

/**
 * Created by Roberto Sales on 01/08/17.
 */
public class IdentityFunction extends NormFunction {
    public IdentityFunction(double[] p) {
        super(p);
    }

    @Override
    public double evaluate(double x) {
        return x;
    }

    @Override
    public int arity() {
        return 0;
    }
}
