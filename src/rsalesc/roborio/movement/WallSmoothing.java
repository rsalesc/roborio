package rsalesc.roborio.movement;

import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public abstract class WallSmoothing {
    private static final double WALL_STICK = 160;

    public static double naive(AxisRectangle field, Point source, double angle, int direction) {
        if(field.contains(source.project(angle, WALL_STICK)))
            return angle;

        double l = 0, r = R.PI;
        while(l+0.06 < r) {
            double mid = (l+r)/2;
            if(field.contains(source.project(angle + mid * direction, WALL_STICK)))
                r = mid;
            else l = mid;
        }

        if(field.contains(source.project(angle + l * direction, WALL_STICK)))
            return angle + l * direction;

        while(!field.contains(source.project(angle, WALL_STICK)))
            angle += 0.05*direction;

        return angle;
    }
}
