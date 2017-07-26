package roborio.gunning;

import robocode.Event;
import robocode.Rules;
import roborio.utils.Point;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class GunFireEvent extends Event {
    private final Gun gun;
    private final Point source;
    private final double power;
    private final double angle;
    private final long time;

    public GunFireEvent(Gun gun, Point source, double angle, double power, long time) {
        this.gun = gun;
        this.source = source;
        this.power = power;
        this.time = time;
        this.angle = angle;
    }

    public Gun getGun() {
        return gun;
    }

    public Point getSource() {
        return source;
    }

    public double getPower() {
        return power;
    }

    @Override
    public long getTime() {
        return time;
    }

    public double getAngle() {
        return angle;
    }

    public double getVelocity() {
        return Rules.getBulletSpeed(power);
    }
}
