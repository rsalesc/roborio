package rsalesc.roborio.utils.geo;

import rsalesc.roborio.utils.R;

import java.util.ArrayList;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class LineSegment {
    public Point p1;
    public Point p2;

    public LineSegment(Point a, Point b) {
        this.p1 = a;
        this.p2 = b;
    }

    public Point middle() {
        return p1.weighted(p2, 0.5);
    }

    public Point[] intersect(Circle circle) {
        Point[] res = new Point[2];
        Point H = (circle.center.subtract(p1)).project(p2.subtract(p1)).add(p1);
        double h = (H.subtract(circle.center)).norm();
        if(circle.radius < h - R.EPSILON)
            return new Point[0];
        Point v = p2.subtract(p1).resized(R.sqrt(sqr(circle.radius) - sqr(h)));
        res[0] = H.add(v);
        res[1] = H.subtract(v);

        ArrayList<Point> inter = new ArrayList<>();
        for(int i = 0; i < res.length; i++) {
            if(isContained(res[i]))
                inter.add(res[i]);
        }
        return inter.toArray(new Point[0]);
    }

    public boolean isContained(Point p) {
        return R.isNear(p.subtract(p1).cross(p.subtract(p2)), 0)
                && p.subtract(p1).dot(p.subtract(p2)) < R.EPSILON;
    }

    private double sqr(double x) {
        return x*x;
    }
}
