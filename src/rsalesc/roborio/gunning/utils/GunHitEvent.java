package rsalesc.roborio.gunning.utils;

import robocode.Bullet;
import robocode.BulletHitEvent;

/**
 * Created by Roberto Sales on 27/07/17.
 */
public class GunHitEvent{
    private Bullet bullet;
    private double energy;
    private String name;

    public GunHitEvent(BulletHitEvent e) {
        bullet = e.getBullet();
        energy = e.getEnergy();
        name = e.getName();
    }

    public Bullet getBullet() {
        return bullet;
    }

    public double getEnergy() {
        return energy;
    }

    public String getName() {
        return name;
    }
}
