package roborio.movement.forces;

import roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class LinearForce extends Force {
    private double alpha;
    private double beta;

    public LinearForce(Point source, double alpha, double beta) {
        super(source);
        setAlpha(alpha);
        setBeta(beta);
    }

    @Override
    public Point getDisplacement(double X, double Y) {
        Point source = new Point(getX(), getY());
        Point robot = new Point(X, Y);
        double distance = source.distance(robot);
        return new Point(source, robot).resized(distance * alpha + beta);
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}
