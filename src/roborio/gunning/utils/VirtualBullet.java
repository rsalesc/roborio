package roborio.gunning.utils;

import robocode.Bullet;
import robocode.Rules;
import roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 09/08/17.
 */
public class VirtualBullet {
    private long time;
    private Bullet bullet;
    private Point source;

    public VirtualBullet(Point source, Bullet bullet, long time) {
        this.bullet = bullet;
        this.time = time;
        this.source = source;
    }

    public double getHeading() {
        return bullet.getHeadingRadians();
    }

    public double getPower() {
        return bullet.getPower();
    }

    public double getVelocity() {
        return Rules.getBulletSpeed(getPower());
    }

    public long getTime() {
        return time;
    }

    public String getName() {
        return bullet.getName();
    }

    public Point getSource() {
        return source;
    }
    public Point getPoint() {
        return new Point(bullet.getX(), bullet.getY());
    }

    public Point travel(long delta) {
        return source.project(getHeading(), getVelocity() * delta);
    }

    public Point project(long now) {
        return travel(now - time);
    }

    public boolean isActive() { return bullet.isActive(); }
}
