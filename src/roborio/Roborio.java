package roborio;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.EnemyTracker;
import roborio.utils.Physics;
import roborio.utils.Point;

import java.awt.*;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class Roborio extends AdvancedRobot {

    public void run() {
        clearLastRoundData();
        recoverLastRoundData();

        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setupColors();
        setupRadar();

        System.out.println(EnemyTracker.getInstance().size());

        while(true) {
            if(getRadarTurnRemaining() == 0)
                setTurnRadarRight(1);

            execute();
        }
    }

    void activateSpinningLock() {
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    void setupRadar() {
        activateSpinningLock();
    }

    void setupColors() {
        setColors(Color.BLACK, Color.YELLOW, Color.BLACK);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        EnemyTracker.getInstance().push(e, this);

        setTurnRight(e.getBearing());
        setTurnGunRight(getHeading() - getGunHeading() + e.getBearing());
        if(Math.abs(getTurnRemaining()) < 10) {
            if(e.getDistance() > 300) {
                setAhead(e.getDistance() * 0.5);
            } else if(e.getDistance() < 100) {
                setBack(e.getDistance() * 2);
            }
        }

        if(Math.abs(getGunTurnRemaining()) < 8) {
            setFire(1.0);
        }

        setTurnRadarRight(getHeading() - getRadarHeading() + e.getBearing());
    }

    public boolean isOn1v1() {
        return getOthers() <= 1;
    }
    public boolean isOnMelee() {
        return !isOn1v1();
    }

    void clearLastRoundData() {
        EnemyTracker.getInstance().clear();
    }

    void recoverLastRoundData() {

    }

    public Point getPoint() {
        return new Point(getX(), getY());
    }

    public void setBackAsFront(double bearing, double distance) {
        double angle = Utils.normalRelativeAngle(bearing - getHeadingRadians());
        double narrowAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(narrowAngle);
        setAhead(distance * (angle == narrowAngle ? 1 : -1));
    }

    public void moveWithBackAsFront(Point dest) {
        setBackAsFront(Physics.absoluteBearing(getPoint(), dest), getPoint().distance(dest));
    }
}
