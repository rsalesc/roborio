package rsalesc.roborio.utils.stats;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.waves.BreakType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roberto Sales on 22/08/17.
 */
public class SegmentedBufferSet {
    private List<SegmentedBuffer> buffers;

    public SegmentedBufferSet() {
        buffers = new ArrayList<>();
    }

    public SegmentedBufferSet add(SegmentedBuffer buffer) {
        if(!buffer.isBuilt())
            buffer.build();
        buffers.add(buffer);
        return this;
    }

    public void mutate(Knn.ConditionMutation mutation) {
        for(SegmentedBuffer buffer : buffers)
            buffer.mutate(mutation);
    }

    public GuessFactorStats getStats(TargetingLog f) {
        int length = buffers.size();
        GuessFactorStats[] stats = new GuessFactorStats[length];
        double[] weights = new double[length];

        int ptr = 0;
        for(SegmentedBuffer buffer : buffers) {
            stats[ptr] = buffer.getStats(f);
            weights[ptr] = buffer.getWeight();
            ptr++;
        }

        return GuessFactorStats.merge(stats, weights);
    }

    public GuessFactorStats getStats(TargetingLog f, Object o) {
        int length = buffers.size();
        GuessFactorStats[] stats = new GuessFactorStats[length];
        double[] weights = new double[length];

        int ptr = 0;
        for(SegmentedBuffer buffer : buffers) {
            if(buffer.isEnabled(o)) {
                stats[ptr] = buffer.getStats(f);
                weights[ptr] = buffer.getWeight();
                ptr++;
            }
        }

        return GuessFactorStats.merge(stats, weights);
    }

    public void log(TargetingLog f, int bucket, BreakType type) {
        for(SegmentedBuffer buffer : buffers) {
            if(type == BreakType.BULLET_HIT && buffer.logsOnHit())
                buffer.log(f, bucket, 1.0);
            else if(type == BreakType.BULLET_BREAK && buffer.logsOnBreak())
                buffer.log(f, bucket, 1.0);
            else if(type == BreakType.VIRTUAL_BREAK && buffer.logsOnVirtual())
                buffer.log(f, bucket, 1.0);
        }
    }

    public void log(TargetingLog f, int bucket, BreakType type, double weight) {
        for(SegmentedBuffer buffer : buffers) {
            if(type == BreakType.BULLET_HIT && buffer.logsOnHit())
                buffer.log(f, bucket, weight);
            else if(type == BreakType.BULLET_BREAK && buffer.logsOnBreak())
                buffer.log(f, bucket, weight);
            else if(type == BreakType.VIRTUAL_BREAK && buffer.logsOnVirtual())
                buffer.log(f, bucket, weight);
        }
    }

    public void logGuessFactor(TargetingLog f, double gf, BreakType type) {
        for(SegmentedBuffer buffer : buffers) {
            if(type == BreakType.BULLET_HIT && buffer.logsOnHit())
                buffer.logGuessFactor(f, gf, 1.0);
            else if(type == BreakType.BULLET_BREAK && buffer.logsOnBreak())
                buffer.logGuessFactor(f, gf, 1.0);
            else if(type == BreakType.VIRTUAL_BREAK && buffer.logsOnVirtual())
                buffer.logGuessFactor(f, gf, 1.0);
        }
    }

    public void logGuessFactor(TargetingLog f, double gf, BreakType type, double weight) {
        for(SegmentedBuffer buffer : buffers) {
            if(type == BreakType.BULLET_HIT && buffer.logsOnHit())
                buffer.logGuessFactor(f, gf, weight);
            else if(type == BreakType.BULLET_BREAK && buffer.logsOnBreak())
                buffer.logGuessFactor(f, gf, weight);
            else if(type == BreakType.VIRTUAL_BREAK && buffer.logsOnVirtual())
                buffer.logGuessFactor(f, gf, weight);
        }
    }

    private class BufferEntry {
        public final SegmentedBuffer buffer;
        public final double weight;

        private BufferEntry(SegmentedBuffer buffer, double weight) {
            this.buffer = buffer;
            this.weight = weight;
        }
    }
}
