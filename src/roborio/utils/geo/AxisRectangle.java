package roborio.utils.geo;

import roborio.utils.R;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class AxisRectangle {
    private double minx;
    private double maxx;
    private double miny;
    private double maxy;

    public AxisRectangle(double minx, double maxx, double miny, double maxy) {
        this.minx = minx;
        this.maxx = maxx;
        this.miny = miny;
        this.maxy = maxy;
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
        return Math.sqrt(
                Math.min(sqr(minx - x), sqr(maxx - x)) +
                Math.min(sqr(miny - y), sqr(maxy - y)));
    }

    public double sqr(double x) {
        return x*x;
    }
}
