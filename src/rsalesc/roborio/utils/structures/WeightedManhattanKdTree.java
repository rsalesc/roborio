package rsalesc.roborio.utils.structures;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class WeightedManhattanKdTree<T> extends KdTree<T> {
    private double[] weights;
    public WeightedManhattanKdTree(double[] weights, Integer sizeLimit) {
        super(weights.length, sizeLimit);
        this.weights = weights;
    }

    public int minkowskiBestHyperplane(KdTree<T> node) {
        int res = 0;
        double best = (node.max[0] - node.min[0]) * weights[0];
        if(Double.isNaN(best)) best = 0;

        for(int i = 1; i < node.dim; i++) {
            double nbest = (node.max[i] - node.min[i]) * weights[i];
            if(Double.isNaN(nbest)) nbest = 0;
            if(nbest > best) {
                best = nbest;
                res = i;
            }
        }

        return res;
    }

    @Override
    public double minkowskiDistance(double[] a, double[] b) {
        double res = 0;
        for(int i = 0; i < this.dim; i++) {
            double acc = Math.abs(a[i]-b[i]) * weights[i];
            if(!Double.isNaN(acc))
                res += acc;
        }
        return res;
    }

    @Override
    public double minkowskiToHyperrect(double[] p, double[] min, double[] max) {
        if(min == null)
            return Double.POSITIVE_INFINITY;

        double res = 0;
        for(int i = 0; i < this.dim; i++) {
            double acc = 0;
            if(p[i] > max[i])
                acc = (p[i] - max[i]) * weights[i];
            if(p[i] < min[i])
                acc = (min[i] - p[i]) * weights[i];
            if(!Double.isNaN(acc))
                res += Math.abs(acc);
        }

        return res;
    }

}
