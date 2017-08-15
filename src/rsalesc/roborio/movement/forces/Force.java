package rsalesc.roborio.movement.forces;

import robocode.Robot;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class Force {
    private double x;
    private double y;

    protected Force(Point source) {
        x = source.x;
        y = source.y;
    }

    public Point getDisplacement(Robot robot) {
        return getDisplacement(robot.getX(), robot.getY());
    }

    public abstract Point getDisplacement(double X, double Y);

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
