package roborio.utils;

import robocode.AdvancedRobot;
import robocode.RobotStatus;
import robocode.StatusEvent;
import robocode.util.Utils;

import static roborio.utils.R.HALF_PI;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class BackAsFrontRobot extends AdvancedRobot {
    private RobotStatus status;

    public BackAsFrontRobot() {
        super();
    }

    @Override
    public void onStatus(StatusEvent e) {
        status = e.getStatus();
    }

    public double getX() {
        return status.getX();
    }

    public double getY() {
        return status.getY();
    }

    public Point getPoint() {
        return new Point(getX(), getY());
    }
    public AxisRectangle getBattleField() {
        return new AxisRectangle(0, getBattleFieldWidth(), 0, getBattleFieldHeight());
    }

    public static double getQuickestTurn(double originalTurn) {
        if(Math.abs(originalTurn) < HALF_PI)
            return originalTurn;
        else
            return Utils.normalRelativeAngle(originalTurn + R.PI);
    }

    public void setBackAsFront(double bearing) {
        setBackAsFront(bearing, Double.POSITIVE_INFINITY);
    }

    public void setBackAsFront(double bearing, double distance) {
        double angle = Utils.normalRelativeAngle(bearing - getHeadingRadians());
        double narrowAngle = getQuickestTurn(angle);
        setTurnRightRadians(R.isNear(distance, 0.0) ? 0 : narrowAngle);
        setAhead(distance * (angle == narrowAngle ? 1 : -1));
    }

    public void moveWithBackAsFront(Point dest, double distance) {
        setBackAsFront(Physics.absoluteBearing(getPoint(), dest), distance);
    }

    public void runAwayWithBackAsFront(Point dest, double distance) {
        setBackAsFront(Physics.absoluteBearing(dest, getPoint()), distance);
    }

    public void moveWithBackAsFront(Point dest) {
        moveWithBackAsFront(dest, getPoint().distance(dest));
    }

    public void doSharpTurning() {
        if(Math.abs(getTurnRemaining()) > 30)
            setMaxVelocity(0);
        else
            setMaxVelocity(8);
    }
}
