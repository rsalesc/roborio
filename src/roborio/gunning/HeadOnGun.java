package roborio.gunning;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyTracker;
import roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class HeadOnGun extends AutomaticGun {
    public HeadOnGun(BackAsFrontRobot robot, boolean isVirtual) {
        super(robot, isVirtual);
    }

    @Override
    public void doGunning() {}

    @Override
    public void onScan(ScannedRobotEvent e) {
        ComplexEnemyRobot enemy = EnemyTracker.getInstance().getLatestState(e);

        double offset = Utils.normalRelativeAngle(getRobot().getHeadingRadians() - getGunHeadingRadians() + e.getBearingRadians());
        setFireToRight(offset, 1.0);
    }
}
