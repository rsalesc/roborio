package roborio.utils.geo;

import robocode.util.Utils;
import roborio.utils.R;

import java.awt.geom.Point2D;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class Point {
    public double x;
    public double y;

    public Point() {
        setX(0.0);
        setY(0.0);
    }

    public Point(double x, double y) {
        setX(x);
        setY(y);
    }

    public Point(Point a, Point b) {
        setX(b.x - a.x);
        setY(b.y - a.y);
    }

    public Point(Point2D.Double p) {
        setX(p.x);
        setY(p.y);
    }

    public Point2D.Double to2D() {
        return new Point2D.Double(x, y);
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double squaredDistance(Point rhs) {
        return (x-rhs.x)*(x-rhs.x) + (y-rhs.y)*(y-rhs.y);
    }

    public double distance(Point rhs) {
        return R.sqrt(squaredDistance(rhs));
    }

    public double manhattanDistance(Point rhs) {
        return Math.abs(x-rhs.x) + Math.abs(y-rhs.y);
    }

    public double chebyshevDistance(Point rhs) {
        return Math.max(Math.abs(x-rhs.x), Math.abs(y-rhs.y));
    }

    public double dot(Point rhs) {
        return x*rhs.x + y*rhs.y;
    }

    public double cross(Point rhs) {
        return x*rhs.y - y*rhs.x;
    }

    public double squaredNorm() {
        return this.dot(this);
    }

    public double norm() {
        return R.sqrt(squaredNorm());
    }

    public Point add(Point rhs) {
        return new Point(x + rhs.x, y + rhs.y);
    }

    public Point subtract(Point rhs) {
        return new Point(x - rhs.x, y - rhs.y);
    }

    public Point multiply(double alpha) {
        return new Point(x * alpha, y * alpha);
    }

    public Point divide(double alpha) {
        return new Point(x / alpha, y / alpha);
    }

    public void scale(double alpha) {
        x *= alpha;
        y *= alpha;
    }

    public boolean isNull() {
        return (x == 0 && y == 0) || squaredNorm() == 0;
    }

    public Point resized(double alpha) {
        if(isNull())
            return new Point();
        return this.divide(norm()).multiply(alpha);
    }

    public Point versor() {
        return resized(1.0);
    }

    public Point reversed() {
        return new Point(-x, -y);
    }


    /** Note that the angle return isn't in
     * an arabic reference system, but is related
     * to the robocode angle system. That's why
     * it's Math.atan2(x, y), not Math.atan2(y, x).
     *
     * @return angle in robocode notation
     */
    public double absoluteBearing() {
        return R.atan2(x, y);
    }

    public Point project(double angle, double length) {
        return new Point(x + R.sin(angle) * length, y + R.cos(angle) * length);
    }

    public double similarity(Point rhs) {
        return dot(rhs) / norm() / rhs.norm();
    }

    public Point rotate(double theta) {
        theta = Utils.normalRelativeAngle(theta);
        return new Point(R.cos(-theta) * x - R.sin(-theta)  * y, R.sin(-theta) * x + R.cos(-theta) * y);
    }

    public Point rotate(double theta, Point pivot) {
        return this.subtract(pivot).rotate(theta).add(pivot);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
