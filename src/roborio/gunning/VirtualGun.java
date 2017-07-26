package roborio.gunning;

import roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public abstract class VirtualGun extends AutomaticGun {
    public VirtualGun(BackAsFrontRobot robot) {
        super(robot, false);
    }
}
