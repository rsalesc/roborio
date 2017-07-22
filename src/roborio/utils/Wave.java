package roborio.utils;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class Wave {
    private Point source;
    private long time;
    private double velocity;

    public Wave(Point source, long time, double velocity) {
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
}
