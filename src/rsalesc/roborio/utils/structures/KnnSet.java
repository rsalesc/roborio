package rsalesc.roborio.utils.structures;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.waves.BreakType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Roberto Sales on 13/08/17.
 */
public class KnnSet<T> {
    private List<Knn<T>> knns;
    private Knn.DistanceWeighter<T> weighter;

    public KnnSet() {
        knns = new ArrayList<>();
    }

    public KnnSet<T> setDistanceWeighter(Knn.DistanceWeighter<T> weighter) {
        this.weighter = weighter;
        return this;
    }

    public void mutate(Knn.ConditionMutation mutation) {
        for(Knn<T> knn : knns)
            knn.mutate(mutation);
    }

    public KnnSet<T> add(Knn<T> knn) {
        if(!knn.isBuilt())
            knn.build();
        knns.add(knn);
        return this;
    }

    public void add(TargetingLog f, T payload) {
        for(Knn<T> knn : knns)
            knn.add(f, payload);
    }

    public void add(TargetingLog f, T payload, BreakType type) {
        for(Knn<T> knn : knns) {
            if(type == BreakType.BULLET_HIT && knn.logsOnHit())
                knn.add(f, payload);
            else if(type == BreakType.BULLET_BREAK && knn.logsOnBreak())
                knn.add(f, payload);
            else if(type == BreakType.VIRTUAL_BREAK && knn.logsOnVirtual())
                knn.add(f, payload);
        }
    }

    public List<Knn.Entry<T>> query(TargetingLog f) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            res.addAll(knn.query(f));
        }

        Collections.sort(res);
        if(weighter != null)
            res = weighter.getWeightedEntries(res);
        
        return res;
    }

    public List<Knn.Entry<T>> query(TargetingLog f, Object o) {
        List<Knn.Entry<T>> res = new ArrayList<>();
        for(Knn<T> knn : knns) {
            if(knn.isEnabled(o))
                res.addAll(knn.query(f));
        }

        Collections.sort(res);
        if(weighter != null)
            res = weighter.getWeightedEntries(res);

        return res;
    }

    public List<Knn<T>> getKnns() {
        return knns;
    }

    public int availableData(Object o) {
        int res = 0;
        for(Knn<T> knn : knns)
            if(knn.isEnabled(o))
                res += knn.size();
        return res;
    }
}
