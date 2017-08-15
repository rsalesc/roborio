package rsalesc.roborio.utils.waves;

import robocode.Bullet;
import rsalesc.roborio.myself.MyLog;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class MyBulletWave extends MyFireWave {
    private final Bullet bullet;
    public MyBulletWave(MyLog log, Bullet bullet) {
        super(log, bullet.getHeadingRadians(), bullet.getVelocity());
        this.bullet = bullet;
    }

    public Bullet getBullet() {
        return bullet;
    }
}
