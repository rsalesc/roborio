package rsalesc.roborio.movement.forces;

import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class DangerWavePoint extends DangerPoint {
    private final PredictedPoint impact;
    public DangerWavePoint(Point point, double danger, PredictedPoint impact) {
        super(point, danger);
        this.impact = impact;
    }


    public PredictedPoint getImpact() {
        return impact;
    }
}
