package roborio.learning.functions;

/**
 * Created by Roberto Sales on 01/08/17.
 */
public class PowerFunction extends NormFunction {

    public PowerFunction(double[] p) {
        super(p);
    }

    @Override
    public double evaluate(double x) {
        return Math.pow(Math.abs(p[0]*x), p[1]);
    }

    @Override
    public int arity() {
        return 2;
    }
}
