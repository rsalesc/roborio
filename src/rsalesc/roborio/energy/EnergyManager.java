package rsalesc.roborio.energy;

import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MyRobot;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public abstract class EnergyManager {
    public abstract double selectPower(MyRobot me, ComplexEnemyRobot enemy, double myScore, double hisScore);
}
