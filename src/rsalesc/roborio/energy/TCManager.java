package rsalesc.roborio.energy;

import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.myself.MyRobot;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class TCManager extends EnergyManager {
    @Override
    public double selectPower(MyRobot me, ComplexEnemyRobot enemy) {
        return 3.0;
    }
}
