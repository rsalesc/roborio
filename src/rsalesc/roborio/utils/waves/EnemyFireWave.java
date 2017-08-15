package rsalesc.roborio.utils.waves;

import robocode.Bullet;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class EnemyFireWave extends EnemyWave {
    public EnemyFireWave(MySnapshot snap, ComplexEnemyRobot robot, double velocity) {
        super(snap, robot, velocity);
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    public double getPower() {
        return (20. - getVelocity()) / 3;
    }

    public boolean wasFiredBy(Bullet bullet, long time) {
        Point hitPoint = new Point(bullet.getX(), bullet.getY());
        return bullet.getName().equals(getEnemy().getName())
                && R.isNear(bullet.getPower(), getPower())
                && R.isNear(getDistanceTraveled(time), hitPoint.distance(getSource()), 50);
    }
}
