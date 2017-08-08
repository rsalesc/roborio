package roborio.utils.geo;

import roborio.utils.R;

import java.util.ArrayList;

/**
 * Created by Roberto Sales on 07/08/17.
 */
public class Circle {
    Point center;
    double radius;

    public Circle(Point center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public boolean isInside(Point point) {
        return center.distance(point) < radius + R.EPSILON;
    }

    public Point[] intersect(AxisRectangle rect) {
        ArrayList<Point> res = new ArrayList<>();
        Point[] minx = intersectAxis(rect.minx, false);
        Point[] maxx = intersectAxis(rect.maxx, false);
        Point[] miny = intersectAxis(rect.miny, true);
        Point[] maxy = intersectAxis(rect.maxy, true);

        if(minx != null) {
            for(Point point : minx) {
                if(rect.contains(point)) {
                    res.add(point);
                }
            }
        }

        if(maxx != null) {
            for(Point point : maxx) {
                if(rect.contains(point)) {
                    res.add(point);
                }
            }
        }

        if(miny != null) {
            for(Point point : miny) {
                if(rect.contains(point)) {
                    res.add(point);
                }
            }
        }

        if(maxy != null) {
            for(Point point : maxy) {
                if(rect.contains(point)) {
                    res.add(point);
                }
            }
        }

        return res.toArray(new Point[0]);
    }

    private Point[] intersectAxis(double coord, boolean isY){
        Point c = isY ? new Point(center.y, center.x) : center;

        if(Math.abs(coord - c.x) > radius + R.EPSILON)
            return null;

        double dy = R.sqrt(sqr(radius) - sqr(coord - c.x));

        Point[] res = new Point[2];
        res[0] = new Point(coord, c.y - dy);
        res[1] = new Point(coord, c.y + dy);

        for(int i = 0; i < 2; i++) {
            if(isY)
                res[i] = new Point(res[i].y, res[i].x);
        }

        return res;
    }

    private double sqr(double x) {
        return x*x;
    }
}
