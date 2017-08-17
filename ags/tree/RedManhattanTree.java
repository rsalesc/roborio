package ags.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roberto Sales on 12/08/17.
 */
public class RedManhattanTree<T> {
    private KdTree.WeightedManhattan<T> t;

    public RedManhattanTree(double[] weights, int sizeLimit) {
        t = new KdTree.WeightedManhattan<>(weights.length, sizeLimit);
        t.setWeights(weights);
    }

    public void add(double[] point, T value) {
        t.addPoint(point, value);
    }

    public ArrayList<rsalesc.roborio.utils.structures.KdTree.Entry<T>> kNN(double[] query, int K, double alpha) {
        if(K == 0)
            return new ArrayList<>();

        List<KdTree.Entry<T>> list = t.nearestNeighbor(query, K, false);

        ArrayList<rsalesc.roborio.utils.structures.KdTree.Entry<T>> res = new ArrayList<>();
        for(KdTree.Entry<T> entry : list) {
            res.add(new rsalesc.roborio.utils.structures.KdTree.Entry<T>(entry.distance, entry.value));
        }

        return res;
    }

    public int size() {
        return t.size();
    }
}
