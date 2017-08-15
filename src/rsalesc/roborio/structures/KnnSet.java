package rsalesc.roborio.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public class KnnSet<T> {
    private List<Knn<T>> knns;

    public KnnSet() {
        knns = new ArrayList<>();
    }

    public KnnSet<T> add(Knn<T> knn) {
        if(!knn.isBuilt())
            knn.build();
        knns.add(knn);
        return this;
    }

    public void add(double[] point, T payload) {
        for(Knn<T> knn : knns)
            knn.add(point, payload);
    }

    public void add(TargetingLog f, T payload) {
        for(Knn<T> knn : knns)
            knn.add(f, payload);
    }

    public List<Knn.Entry<T>> query(double[] point) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            res.addAll(knn.query(point));
        }

        Collections.sort(res);
        return res;
    }

    public List<Knn.Entry<T>> query(TargetingLog f) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            res.addAll(knn.query(f));
        }

        Collections.sort(res);
        return res;
    }

    public List<Knn<T>> getKnns() {
        return knns;
    }
}
