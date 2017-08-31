package rsalesc.roborio.utils;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;

import java.io.IOException;
import java.io.PrintStream;

import static rsalesc.roborio.utils.R.HALF_PI;

/**
 * Created by Roberto Sales on 22/07/17.
 * TODO: prediction with sharp turning not working
 */
public abstract class BackAsFrontRobot extends AdvancedRobot {
    public static final boolean SHARP_TURNING = false;

    private int idle = 0;

    private RobotStatus status;
    private AxisRectangle field;
    private double _maxVelocity = Rules.MAX_VELOCITY;
    private boolean sharpened = false;
    private double _velocityBeforeSharp;

    public void dissociate() {
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
    }

    @Override
    public void onStatus(StatusEvent e) {
        if(sharpened) {
            super.setMaxVelocity(_velocityBeforeSharp);
            _maxVelocity = _velocityBeforeSharp;
            sharpened = false;
        }

        if(status != null && status.getGunHeat() == 0 && status.getGunHeat() == e.getStatus().getGunHeat())
            idle++;
        else
            idle = 0;

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

    public long getRealTime() {
        return super.getTime();
    }

    @Override
    public int getRoundNum() {
        return status.getRoundNum();
    }

    public BattleTime getBattleTime() {
        return new BattleTime(getTime(), getRoundNum());
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

    @Override
    public double getGunHeading() {
        return status.getGunHeading();
    }

    @Override
    public void setMaxVelocity(double x) {
        if(!sharpened) {
            _maxVelocity = x;
            super.setMaxVelocity(x);
        } else {
            _velocityBeforeSharp = x;
        }
    }

    public double getMaxVelocity() {
        return _maxVelocity;
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
        bearing = Utils.normalAbsoluteAngle(bearing);
        double angle = Utils.normalRelativeAngle(bearing - getHeadingRadians());
        double narrowAngle = getQuickestTurn(angle);
        setTurnRightRadians(R.isNear(distance, 0.0) ? 0 : narrowAngle);
        setAhead(distance * (angle == narrowAngle ? 1 : -1));
        if(SHARP_TURNING) doSharpTurning();
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

    public double getMaxTurning() {
        return Physics.maxTurningRate(getVelocity());
    }

    public void setTurnTo(double radians) {
        radians = Utils.normalAbsoluteAngle(radians);
        double offset = Utils.normalRelativeAngle(radians - getHeadingRadians());
        setTurnRightRadians(offset);
    }

    private void doSharpTurning() {
        double maxTurn = getMaxTurning();
        Range turnRange = new Range(-maxTurn, +maxTurn);
        if(!turnRange.isNearlyContained(getTurnRemainingRadians())) {
            if(!sharpened)
                _velocityBeforeSharp = _maxVelocity;
            sharpened = true;
            super.setMaxVelocity(0);
        }
    }

    public void setGunTo(double radians) {
        radians = Utils.normalAbsoluteAngle(radians);
        double offset = Utils.normalRelativeAngle(radians - getGunHeadingRadians());
        setTurnGunRightRadians(offset);
    }

    public AxisRectangle getHitBox() {
        return new AxisRectangle(getX() - 18, getX() + 18, getY() - 18, getY() + 18);
    }

    public double getNewVelocity() {
        return MovementPredictor.getNewVelocity(getVelocity(), getMaxVelocity(), getDistanceRemaining());
    }

    public double getNewHeading() {
        return MovementPredictor.getNewHeading(getHeadingRadians(), getVelocity(), getTurnRemainingRadians());
    }

    public Point getNextPosition() {
        return getPoint().project(getNewHeading(), getNewVelocity());
    }

    public int getTicksToCool() {
        return (int) Math.ceil(getGunHeat() / getGunCoolingRate());
    }

    /**
     * get the number of complete ticks with gunheat == 0
     */
    public int getIdleTicks() {
        return idle;
    }

    public void handle(Exception e) {
        System.out.println("got an exception");
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
