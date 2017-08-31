package rsalesc.roborio.utils.waves;

import robocode.Bullet;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MySnapshot;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public class EnemyFireWave extends EnemyWave {
    public boolean heatWave = false;

    public EnemyFireWave(MySnapshot snap, ComplexEnemyRobot robot, double velocity) {
        super(snap, robot, velocity);
    }

    public EnemyFireWave imaginary() {
        this.heatWave = true;
        return this;
    }

    public EnemyFireWave fired() {
        this.heatWave = false;
        return this;
    }

    public boolean isImaginary() {
        return this.heatWave;
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
                && R.isNear(getDistanceTraveled(time), hitPoint.distance(getSource()), 52);
    }
}
