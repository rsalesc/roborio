package rsalesc.roborio.utils.geo;

import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class AxisRectangle {
    public double minx;
    public double maxx;
    public double miny;
    public double maxy;

    public AxisRectangle(double minx, double maxx, double miny, double maxy) {
        this.minx = minx;
        this.maxx = maxx;
        this.miny = miny;
        this.maxy = maxy;
    }

    public AxisRectangle(Point center, double size) {
        this.minx = center.x - size / 2;
        this.maxx = center.x + size / 2;
        this.miny = center.y - size / 2;
        this.maxy = center.y + size / 2;
    }

    public void add(Point point) {
        minx = Math.min(minx, point.getX());
        maxx = Math.max(maxx, point.getX());
        miny = Math.max(miny, point.getY());
        maxy = Math.max(maxy, point.getY());
    }

    public double getWidth() {
        return maxx - minx;
    }

    public double getHeight() {
        return maxy - miny;
    }

    public Point getCenter() {
        return new Point((minx+maxx)/2, (miny+maxy)/2);
    }

    public boolean contains(Point point) {
        return R.nearOrBetween(minx, point.getX(), maxx)
                && R.nearOrBetween(miny, point.getY(), maxy);
    }

    public boolean strictlyContains(Point point) {
        return R.strictlyBetween(minx, point.getX(), maxx)
                && R.strictlyBetween(miny, point.getY(), maxy);
    }

    public AxisRectangle shrinkX(double amount) {
        if(amount*2 > getWidth()) {
            double x = (minx + maxx) / 2;
            return new AxisRectangle(x, x, miny, maxy);
        } else {
            return new AxisRectangle(minx + amount, maxx - amount, miny, maxy);
        }
    }

    public AxisRectangle shrinkY(double amount) {
        if(amount*2 > getHeight()) {
            double y = (miny + maxy) / 2;
            return new AxisRectangle(minx, maxx, y, y);
        } else {
            return new AxisRectangle(minx, maxx, miny + amount, maxy - amount);
        }
    }

    public AxisRectangle shrink(double amountX, double amountY) {
        return this.shrinkX(amountX).shrinkY(amountY);
    }

    public double distance(Point point) {
        double x = point.getX();
        double y = point.getY();
        return R.sqrt(
                Math.min(sqr(minx - x), sqr(maxx - x)) +
                Math.min(sqr(miny - y), sqr(maxy - y)));
    }

    public double sqr(double x) {
        return x*x;
    }

    public Point[] getCorners() {
        Point[] corners = new Point[4];
        corners[0] = new Point(minx, miny);
        corners[1] = new Point(maxx, miny);
        corners[2] = new Point(maxx, maxy);
        corners[3] = new Point(minx, maxy);
        return corners;
    }

    public AxisRectangle transposed() {
        return new AxisRectangle(miny, maxy, minx, maxx);
    }
}
