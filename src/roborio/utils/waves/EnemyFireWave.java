package roborio.utils.waves;

import robocode.Bullet;
import roborio.enemies.ComplexEnemyRobot;
import roborio.myself.MySnapshot;
import roborio.utils.Point;
import roborio.utils.R;

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
