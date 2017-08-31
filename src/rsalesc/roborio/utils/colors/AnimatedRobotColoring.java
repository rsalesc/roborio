package rsalesc.roborio.utils.colors;

import rsalesc.roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 29/08/17.
 */
public class AnimatedRobotColoring {
    BackAsFrontRobot robot;
    Gradient gradient;
    long period;

    public AnimatedRobotColoring(BackAsFrontRobot robot, Gradient gradient, long period) {
        this.robot = robot;
        this.gradient = gradient;
        this.period = period;
    }

    public void tick(long time) {
        double alpha = (double) (time % period) / period;

        robot.setAllColors(gradient.evaluate(alpha));
    }
}
