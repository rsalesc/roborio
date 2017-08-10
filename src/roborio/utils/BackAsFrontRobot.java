package roborio.utils;

import robocode.AdvancedRobot;
import robocode.RobocodeFileOutputStream;
import robocode.RobotStatus;
import robocode.StatusEvent;
import robocode.util.Utils;
import roborio.utils.geo.AxisRectangle;
import roborio.utils.geo.Point;

import java.io.IOException;
import java.io.PrintStream;

import static roborio.utils.R.HALF_PI;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class BackAsFrontRobot extends AdvancedRobot {
    private RobotStatus status;
    private AxisRectangle field;

    public BackAsFrontRobot() {
        super();
    }

    @Override
    public void onStatus(StatusEvent e) {
        status = e.getStatus();
        field = _getBattleField();
    }

    public double getX() {
        return status.getX();
    }

    public double getY() {
        return status.getY();
    }

    @Override
    public double getHeadingRadians() {
        return status.getHeadingRadians();
    }

    @Override
    public double getHeading() {
        return status.getHeading();
    }

    @Override
    public long getTime() {
        return status.getTime();
    }

    @Override
    public double getEnergy() {
        return status.getEnergy();
    }

    @Override
    public double getVelocity() {
        return status.getVelocity();
    }

    @Override
    public int getOthers() {
        return status.getOthers();
    }

    @Override
    public double getGunHeat() {
        return status.getGunHeat();
    }

    @Override
    public double getGunHeadingRadians() {
        return status.getGunHeadingRadians();
    }

    public Point getPoint() {
        return new Point(getX(), getY());
    }
    private AxisRectangle _getBattleField() {
        return new AxisRectangle(0, getBattleFieldWidth(), 0, getBattleFieldHeight());
    }

    public AxisRectangle getBattleField() {
        return field;
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

    public AxisRectangle getHitBox() {
        return new AxisRectangle(getX() - 18, getX() + 18, getY() - 18, getY() + 18);
    }

    public Point impreciseNextPosition() {
        double maxTurn = Physics.maxTurningRate(getVelocity());
        double turn = R.constrain(-maxTurn, getTurnRemainingRadians(), +maxTurn);
        double absHeading = Utils.normalAbsoluteAngle(getHeadingRadians() + turn);

        return new Point(getX() + Math.sin(absHeading) * getVelocity(),
                getY() + Math.cos(absHeading) * getVelocity());
    }

    public void handle(Exception e) {
        e.printStackTrace();

        try{
            PrintStream out = new PrintStream(new RobocodeFileOutputStream(getDataFile((int)(Math.random()*1000) + ".error")));
            e.printStackTrace(out);
            out.flush();
            out.close();
        }
        catch (IOException ioex){}
    }
}
