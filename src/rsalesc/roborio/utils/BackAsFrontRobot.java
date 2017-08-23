package rsalesc.roborio.utils;

import robocode.*;
import robocode.util.Utils;
import rsalesc.roborio.movement.predictor.MovementPredictor;
import rsalesc.roborio.myself.MyLog;
import rsalesc.roborio.utils.geo.AxisRectangle;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;

import java.io.IOException;
import java.io.PrintStream;

import static rsalesc.roborio.utils.R.HALF_PI;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public abstract class BackAsFrontRobot extends AdvancedRobot {
    public static final boolean SHARP_TURNING = true;

    private RobotStatus status;
    private AxisRectangle field;
    private double _maxVelocity = Rules.MAX_VELOCITY;
    private boolean sharpened = false;
    private double _velocityBeforeSharp;

    @Override
    public void onStatus(StatusEvent e) {
        if(sharpened) {
            super.setMaxVelocity(_velocityBeforeSharp);
            _maxVelocity = _velocityBeforeSharp;
            sharpened = false;
        }

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
    public double getDistanceRemaining() {
        return status.getDistanceRemaining();
    }

    @Override
    public double getGunTurnRemainingRadians() {
        return status.getGunTurnRemainingRadians();
    }

    @Override
    public double getGunTurnRemaining() {
        return status.getGunTurnRemaining();
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
    public double getTurnRemainingRadians() {
        return status.getTurnRemainingRadians();
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

    public AxisRectangle getHitBox() {
        return new AxisRectangle(getX() - 18, getX() + 18, getY() - 18, getY() + 18);
    }

    public Point getNextPosition() {
        // safely assume that if the bot is not moving, it will keep this way
        // its ok to do this because even if it moves it displacement will be really small
        // and prediction will correct any mistake in the next ticks
        if(getVelocity() == 0)
            return getPoint();

        double maxTurn = Physics.maxTurningRate(getVelocity());
        double turn = R.constrain(-maxTurn, getTurnRemainingRadians(), +maxTurn);
        double absHeading = Utils.normalAbsoluteAngle(getHeadingRadians() + turn);
        double remaining = getDistanceRemaining();
        int ahead = MyLog.getInstance().getLatest().getAhead();

        if(ahead == 0)
            return getPoint();

        double newVelocity = MovementPredictor.getNewVelocity(getVelocity(), getMaxVelocity(), ahead, remaining);
        return getPoint().project(absHeading, newVelocity);
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
