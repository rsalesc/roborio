package roborio.movement.forces;

import roborio.utils.geo.Point;
import roborio.utils.R;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class SigmoidForce extends Force {
    private static final double     STEEP = -50.0;
    private double alpha;
    private double threshold;
    private double beta;

    public SigmoidForce(Point source, double alpha, double threshold, double beta) {
        super(source);
        setAlpha(alpha);
        setThreshold(threshold);
        setBeta(beta);
    }

    public SigmoidForce(Point source, double alpha, double threshold) {
        this(source, alpha, threshold, 0.0);
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public Point getDisplacement(double X, double Y) {
        Point source = new Point(getX(), getY());
        Point robot = new Point(X, Y);
        double distance = source.distance(robot);
        double size = R.logisticFunction(distance, threshold, STEEP) * alpha + beta;
        return new Point(source, robot).resized(size);
    }
}
