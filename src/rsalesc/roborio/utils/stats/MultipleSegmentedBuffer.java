package rsalesc.roborio.utils.stats;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.MultipleSlicingStrategy;
import rsalesc.roborio.utils.structures.Knn;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Roberto Sales on 23/08/17.
 * TODO: optimize sparse segments
 */
public class MultipleSegmentedBuffer extends SegmentedBuffer {
    private MultipleSlicingStrategy multiStrategy;
    private ArrayList<Segmentation<GuessFactorStats>> segmentations;
    private int bufferCount;
    private double[] segmentationWeights;
    private double[] sliceCountWeights;
    private boolean _weightsSegments = false;

    private Knn.ParametrizedCondition segmentWeighterCondition;

    public MultipleSegmentedBuffer() {
        super();
    }

    public MultipleSegmentedBuffer setMultipleStrategy(MultipleSlicingStrategy strategy) {
        multiStrategy = strategy;
        return this;
    }

    public MultipleSegmentedBuffer weightsSegments() {
        _weightsSegments = true;
        return this;
    }

    @Override
    public SegmentedBuffer build() {
        built = true;
        segmentations = new ArrayList<>();
        for(double[][] slices : multiStrategy.getSlices()) {
            segmentations.add(new Segmentation<>(slices));
        }

        bufferCount = segmentations.size();
        segmentationWeights = new double[bufferCount];
        Arrays.fill(segmentationWeights, 1.0);

        sliceCountWeights = new double[bufferCount];

        int ptr = 0;
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            sliceCountWeights[ptr++] = segmentation.getSliceCount();
        }

        return this;
    }

    protected GuessFactorStats makeStats(Segmentation<GuessFactorStats> segmentation) {
        if(hasDepth)
            return new GuessFactorStats(rollingDepth);

        // TODO: make this strategy more generic
        int sliceCount = segmentation.getSliceCount();
        double depth;
        if(sliceCount < 2)
            depth = 3;
        else if(sliceCount < 5)
            depth = 1;
        else if(sliceCount < 10)
            depth = 0.6;
        else if(sliceCount < 30)
            depth = 0.3;
        else
            depth = 0.1;

        return new GuessFactorStats(depth);
    }

    private GuessFactorStats getStatsFrom(Segmentation<GuessFactorStats> segmentation, double[] vals) {
        GuessFactorStats stats = segmentation.get(vals);
        if (stats == null) {
            stats = makeStats(segmentation);
            segmentation.add(vals, stats);
        }

        return stats;
    }

    @Override
    public GuessFactorStats getStats(double[] vals) {
        GuessFactorStats[] sts = new GuessFactorStats[bufferCount];

        int ptr = 0;
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            sts[ptr++] = getStatsFrom(segmentation, vals);
        }

        return GuessFactorStats.merge(sts, _weightsSegments ? sliceCountWeights : segmentationWeights);
    }

    public GuessFactorStats getStats(TargetingLog f) {
        return getStats(multiStrategy.getQuery(f));
    }

    @Override
    public void log(TargetingLog f, int bucket, double weight) {
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            getStatsFrom(segmentation, multiStrategy.getQuery(f)).add(bucket, weight);
        }
    }

    @Override
    public void log(TargetingLog f, int bucket) {
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            getStatsFrom(segmentation, multiStrategy.getQuery(f)).add(bucket, 1.0);
        }
    }

    @Override
    public void logGuessFactor(TargetingLog f, double gf, double weight) {
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            getStatsFrom(segmentation, multiStrategy.getQuery(f)).logGuessFactor(gf, weight);
        }
    }

    @Override
    public void logGuessFactor(TargetingLog f, double gf) {
        for(Segmentation<GuessFactorStats> segmentation : segmentations) {
            getStatsFrom(segmentation, multiStrategy.getQuery(f)).logGuessFactor(gf, 1.0);
        }
    }
}
