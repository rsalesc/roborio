package rsalesc.roborio.gunning.utils;

import robocode.Bullet;
import robocode.Rules;
import rsalesc.roborio.gunning.Gun;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class GunFireEvent{
    private final Gun gun;
    private final Point source;
    private final long time;
    private final Bullet bullet;

    private final double angle;
    private final double power;

    public GunFireEvent(Gun gun, Point source, Bullet bullet, long time) {
        this.gun = gun;
        this.source = source;
        this.time = time;
        this.bullet = bullet;
        this.angle = bullet.getHeadingRadians();
        this.power = bullet.getPower();
    }

    public GunFireEvent(Gun gun, Point source, double angle, double power, long time) {
        this.gun = gun;
        this.source = source;
        this.time = time;
        this.bullet = null;
        this.angle = angle;
        this.power = power;
    }

    public boolean isVirtual() {
        return bullet == null;
    }

    public Bullet getBullet() {
        return bullet;
    }

    public Gun getGun() {
        return gun;
    }

    public Point getSource() {
        return source;
    }

    public double getPower() {
        return bullet == null ? power : bullet.getPower();
    }

    public long getTime() {
        return time;
    }

    public double getAngle() {
        return bullet == null ? angle : bullet.getHeadingRadians();
    }

    public double getVelocity() {
        return bullet == null ? Rules.getBulletSpeed(getPower()) : bullet.getVelocity();
    }
}
