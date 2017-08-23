package rsalesc.roborio.utils.stats;

/**
 * Created by Roberto Sales on 27/07/17.
 */
public class Segmentation<T extends Stats> {
    private int                 pieces;
    private int                 depth;
    private double[][]          slices;
    private SegmentTrie<T>      stats;

    public Segmentation(double[][] slices) {
        this.slices = slices;
        depth = slices.length;
        int[] sizes = new int[depth];
        for(int i = 0; i < depth; i++) {
            sizes[i] = slices[i].length + 1;
            pieces += sizes[i];
        }
        stats = new SegmentTrie<>(sizes);
    }

    public void addFromSegments(int[] segs, T payload) {
        if(segs.length != depth)
            throw new IllegalArgumentException();

        stats.add(segs, payload);
    }

    public void add(double[] values, T payload) {
        if(values.length != depth)
            throw new IllegalArgumentException();

        addFromSegments(valuesToSegments(values), payload);
    }

    public T getFromSegments(int[] segs) {
        if(segs.length != depth)
            throw new IllegalArgumentException();

        return stats.get(segs);
    }

    public T get(double[] values) {
        if(values.length != depth)
            throw new IllegalArgumentException();

        return getFromSegments(valuesToSegments(values));
    }

    private int[] valuesToSegments(double[] values) {
        int[] query = new int[depth];
        for(int i = 0; i < depth; i++) {
            int l = -1, r = slices[i].length - 1;
            while(l < r) {
                int mid = (l + r + 1) / 2;
                if(slices[i][mid] <= values[i])
                    l = mid;
                else r = mid - 1;
            }

            query[i] = l + 1;
        }

        return query;
    }

    public int getSliceCount() {
        return pieces;
    }
}
