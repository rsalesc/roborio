package roborio.movement.forces;

import roborio.utils.Point;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public class DangerPoint extends Point {
    private double  danger;

    public DangerPoint(double x, double y, double danger) {
        super(x, y);
        this.danger = danger;
    }

    public DangerPoint(Point point, double danger) {
        this(point.x, point.y, danger);
    }

    public double getDanger() {
        return danger;
    }

    public void setDanger(double danger) {
        this.danger = danger;
    }
}