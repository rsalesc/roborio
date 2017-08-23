package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 18/08/17.
 */
public class MatrixUtils {
    public static double[] getColumn(double[][] m, int col) {
        if(m == null)
            throw new IllegalStateException();

        double[] res = new double[m.length];
        for(int i = 0; i < m.length; i++)
            res[i] = m[i][col];

        return res;
    }
}
