package rsalesc.roborio.movement.forces;

import rsalesc.roborio.movement.predictor.PredictedPoint;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class GotoSurfingCandidate extends DangerPoint {
    private final PredictedPoint passPoint;
    public GotoSurfingCandidate(Point point, double danger, PredictedPoint impact) {
        super(point, danger);
        this.passPoint = impact;
    }


    public PredictedPoint getPassPoint() {
        return passPoint;
    }
}
