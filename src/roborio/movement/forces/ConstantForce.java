package roborio.movement.forces;

import roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class ConstantForce extends Force {
    private double alpha;

    public ConstantForce(Point source, double alpha) {
        super(source);
        setAlpha(alpha);
    }

    @Override
    public Point getDisplacement(double X, double Y) {
        return new Point(X - getX(), Y - getY()).resized(alpha);
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}
