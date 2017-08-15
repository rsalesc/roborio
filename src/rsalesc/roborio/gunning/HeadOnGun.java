package rsalesc.roborio.gunning;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class HeadOnGun extends AutomaticGun {
    public HeadOnGun(BackAsFrontRobot robot) {
        super(robot, false);
    }

    @Override
    protected void buildStructure() {}

    @Override
    public double wouldHit() {
        return 0;
    }

    @Override
    public double wouldHitPower() {
        return 0;
    }

    @Override
    public void doGunning() {}

    @Override
    public String getName() {
        return "HeadOnGun";
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        ComplexEnemyRobot enemy = EnemyTracker.getInstance().getLatestState(e);

        double offset = Utils.normalRelativeAngle(getRobot().getHeadingRadians() - getGunHeadingRadians() + e.getBearingRadians());
        setFireToRight(offset, 1.0);
    }
}
