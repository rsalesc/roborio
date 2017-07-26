package roborio.gunning.utils;

import roborio.enemies.ComplexEnemyRobot;
import roborio.myself.MyRobot;
import roborio.utils.R;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public abstract class PowerSelection {

    public static double naive(MyRobot robot, ComplexEnemyRobot enemy, double confidence) {
        double basePower = 1.75;
        if(confidence > 0.8)
            basePower = 2.6;

        double distance = robot.getPoint().distance(enemy.getPoint());
        double myEnergy = robot.getEnergy();
        double hisEnergy = enemy.getEnergy();

        if(hisEnergy < myEnergy) {
            basePower += 0.5;
        }

        if(distance < 200)
            basePower += (distance - 200) / 300;
        else
            basePower -= Math.min((distance - 200) / 450, 1.0);

        double power = R.constrain(0.1, basePower, 3.0);

        if(myEnergy < 10)
            power /= 4;

        return R.constrain(0.1, power, 3.0);
    }
}
