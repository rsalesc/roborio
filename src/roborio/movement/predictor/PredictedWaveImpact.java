package roborio.movement.predictor;

import roborio.utils.geo.Range;
import roborio.utils.waves.Wave;

import java.util.ArrayList;

/**
 * Created by Roberto Sales on 07/08/17.
 */
public class PredictedWaveImpact {
    private final PredictedPoint initial;
    private final ArrayList<PredictedPoint> points;
    private final Range range;
    private final Wave wave;

    public PredictedWaveImpact(Wave wave, PredictedPoint initial, ArrayList<PredictedPoint> points, Range range) {
        this.wave = wave;
        this.initial = initial;
        this.points = points;
        this.range = range;
    }

    public PredictedPoint getInitialPoint() {
        return initial;
    }

    public ArrayList<PredictedPoint> getPoints() {
        return points;
    }

    public Range getIntersection() {
        return range;
    }

    public Wave getWave() { return wave; }

    public PredictedPoint getLastImpactPoint() {
        if(this.points.size() == 0)
            return null;
        return this.points.get(this.points.size() - 1);
    }

    public PredictedPoint getMidwayImpactPoint() {
        for(PredictedPoint point : points) {
            if(wave.hasPassed(point, point.time)) {
                return point;
            }
        }

        return null;
    }

    public PredictedPoint getFirstImpactPoint() {
        for(PredictedPoint point : points) {
            if(wave.hasTouchedRobot(point, point.time)) {
                return point;
            }
        }

        return null;
    }
}
