package roborio.utils;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class MathUtils {
    public static double logisticFunction(double x, double x0, double k) {
        return 1.0 / (1.0 + Math.exp(-k * (x-x0)));
    }
    public static double logisticFunction(double x) {
        return logisticFunction(x, 0.0, 1.0);
    }
}
