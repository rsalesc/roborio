package roborio.utils.waves;

import roborio.enemies.ComplexEnemyRobot;
import roborio.myself.MyRobot;
import roborio.myself.MySnapshot;
import roborio.utils.Physics;
import roborio.utils.Point;
import roborio.utils.R;

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

    public double getDistanceTraveled(long time) {
        return velocity * (time - this.time);
    }

    public boolean hasTouchedRobot(Point point, long time) {
        return point.distance(source) - Physics.BOT_WIDTH < getDistanceTraveled(time);
    }

    public boolean hasPassedRobot(MyRobot robot) {
        return robot.getPosition().distance(source) + R.WAVE_EXTRA < getDistanceTraveled(robot.getTime());
    }

    public boolean hasPassedRobot(ComplexEnemyRobot robot) {
        return robot.getPoint().distance(source) + R.WAVE_EXTRA < getDistanceTraveled(robot.getTime());
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
}
