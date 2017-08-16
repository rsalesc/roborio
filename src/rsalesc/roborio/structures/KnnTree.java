package rsalesc.roborio.structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public class KnnTree<T> extends Knn<T> {
    public enum Mode {
        MANHATTAN,
        EUCLIDEAN
    }

    private Mode mode;
    private Integer limit = null;
    private KdTree<T> tree;

    public KnnTree<T> setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public KnnTree<T> setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public void buildStructure() {
        if(mode == Mode.MANHATTAN)
            tree = new WeightedManhattanKdTree<>(this.getStrategy().getWeights(), limit);
        else if(mode == Mode.EUCLIDEAN)
            tree = new WeightedEuclideanKdTree<>(this.getStrategy().getWeights(), limit);
        else
            throw new IllegalStateException("building with no valid mode specified");
    }

    @Override
    public int size() {
        return tree.size();
    }

    @Override
    public void add(double[] point, T payload) {
        tree.add(point, payload);
    }

    @Override
    public List<Entry<T>> query(double[] point, int K, double alpha) {
        List<Entry<T>> res = new ArrayList<>();
        for(KdTree.Entry<T> entry : tree.kNN(point, K, alpha)) {
            res.add(makeEntry(entry.distance, entry.payload));
        }

        return res;
    }
}
