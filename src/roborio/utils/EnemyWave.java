package roborio.utils;

import roborio.enemies.ComplexEnemyRobot;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class EnemyWave extends Wave {
    private ComplexEnemyRobot enemy;

    public EnemyWave(ComplexEnemyRobot robot, double velocity) {
        super(robot.getPoint(), robot.getTime(), velocity);
        setEnemy(robot);
    }

    public ComplexEnemyRobot getEnemy() {
        return enemy;
    }

    public void setEnemy(ComplexEnemyRobot enemy) {
        this.enemy = enemy;
    }
}
