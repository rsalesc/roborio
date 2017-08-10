package roborio.gunning.utils;

import roborio.enemies.ComplexEnemyRobot;
import roborio.myself.MyRobot;
import roborio.utils.R;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public abstract class PowerSelection {

    public static double naive(MyRobot robot, ComplexEnemyRobot enemy, double confidence) {
//        return simple(robot, enemy, confidence);
        return 3.0;
    }

    public static double simple(MyRobot robot, ComplexEnemyRobot enemy, double confidence) {
        return R.constrain(0.1, (150. / robot.getPoint().distance(enemy.getPoint()))
                    + Math.min(Math.min(enemy.getEnergy() / 4, robot.getEnergy() / 16), 2), 3.0);
    }

    public static double mine(MyRobot robot, ComplexEnemyRobot enemy, double confidence) {

        double distance = robot.getPoint().distance(enemy.getPoint());
        double myEnergy = robot.getEnergy();
        double hisEnergy = enemy.getEnergy();
        double basePower = 1.75;

        if(hisEnergy+1 < myEnergy && myEnergy < 60
                && (hisEnergy * 2.5 >= myEnergy || myEnergy < 15)) {
            basePower -= (myEnergy - hisEnergy) / 50 * 0.3;
        } else {
            basePower = 2.5;
            if(distance > 500)
                basePower -= (distance - 500) / 300;
        }

        if(distance < 300)
            basePower += (200 - distance) / 300;

        double power = R.constrain(0.1, basePower, 3.0);

        if(myEnergy < 5)
            power /= 4;
        else if(myEnergy < 10)
            power /= 3;

        if(myEnergy < 0.5)
            return 0;

        return R.constrain(0.1, Math.min(power, hisEnergy * 0.25), 3.0);
    }
}
