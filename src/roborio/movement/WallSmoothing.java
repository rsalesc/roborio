package roborio.movement;

import roborio.utils.AxisRectangle;
import roborio.utils.Point;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public abstract class WallSmoothing {
    private static final double WALL_STICK = 125;

    public static double naive(AxisRectangle field, Point source, double angle, int direction) {
        while(!field.contains(source.project(angle, WALL_STICK)))
        angle += direction*0.05;
        return angle;
    }
}
