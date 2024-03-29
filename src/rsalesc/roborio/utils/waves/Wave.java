package rsalesc.roborio.utils.waves;

import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.geo.Circle;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class Wave {
    private Point source;
    private long time;
    private double velocity;
    private MySnapshot snapshot;

    public Wave(MySnapshot snapshot, Point source, long time, double velocity) {
        setSnapshot(snapshot);
        setSource(source);
        setTime(time);
        setVelocity(velocity);
    }

    public Point getSource() {
        return source;
    }

    public void setSource(Point source) {
        this.source = source;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public boolean isEnemyWave() {
        return false;
    }

    public boolean isRobotWave() {
        return false;
    }

    public boolean isMyWave() {
        return false;
    }

    public boolean isReal() { return !isVirtual(); }

    public boolean isVirtual() { return true; }

    public boolean hasStarted(long time) {
        return time >= this.time;
    }

    public double getDistanceTraveled(long time) {
        return Math.max(velocity * (time - this.time), 0);
    }

    public boolean hasTouchedRobot(Point point, long time) {
        return point.distance(source) - Physics.BOT_WIDTH <= getDistanceTraveled(time);
    }

    public boolean hasPassedRobot(MyRobot robot) {
        return robot.getPosition().distance(source) + Physics.BOT_WIDTH < getDistanceTraveled(robot.getTime());
    }

    public boolean hasPassedRobot(ComplexEnemyRobot robot) {
        return robot.getPoint().distance(source) + Physics.BOT_WIDTH < getDistanceTraveled(robot.getTime());
    }

    public boolean hasPassedRobot(Point point, long time) {
        return point.distance(source) + Physics.BOT_WIDTH < getDistanceTraveled(time);
    }

    public boolean hasPassed(Point point, long time) {
        return point.distance(source) < getDistanceTraveled(time);
    }

    public double getBreakTime(Point dest) {
        return dest.distance(source) / velocity + time;
    }

    public double getBreakTime(ComplexEnemyRobot robot) {
        return robot.getPoint().distance(source) / velocity + time;
    }

    public double getBreakTime(MyRobot robot) {
        return robot.getPoint().distance(source) / velocity + time;
    }

    public int getMyDirection() {
        return snapshot.getOffset(-1).getDirection(getSource());
    }

    public MySnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(MySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public double getAngle(Point point) {
        return Physics.absoluteBearing(source, point);
    }

    public Circle getCircle(long time) {
        return new Circle(source, getDistanceTraveled(time));
    }

    public Point project(double angle, long time) {
        return getSource().project(angle, getDistanceTraveled(time));
    }
}
