package rsalesc.roborio.utils.waves;

import robocode.ScannedRobotEvent;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MySnapshot;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class EnemyWave extends RobotWave {
    private ComplexEnemyRobot enemy;

    public EnemyWave(MySnapshot snap, ComplexEnemyRobot robot, double velocity) {
        super(snap, robot.getPoint(), robot.getTime(), velocity);
        setEnemy(robot);
    }

    public ComplexEnemyRobot getEnemy() {
        return enemy;
    }

    public void setEnemy(ComplexEnemyRobot enemy) {
        this.enemy = enemy;
    }

    @Override
    public boolean isEnemyWave() {
        return true;
    }

    public boolean isFrom(String name) {
        return enemy.getName().equals(name);
    }

    public boolean isFrom(ComplexEnemyRobot robot) {
        return isFrom(robot.getName());
    }

    public boolean isFrom(ScannedRobotEvent e) {
        return isFrom(e.getName());
    }
}
