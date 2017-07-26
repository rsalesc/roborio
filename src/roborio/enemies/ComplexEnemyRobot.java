package roborio.enemies;

import robocode.ScannedRobotEvent;
import roborio.movement.predictor.MovementPredictor;
import roborio.utils.AxisRectangle;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.Point;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class ComplexEnemyRobot extends EnemyRobot {
    private long time;

    private double absBearing;
    private double x;
    private double y;

    private double angularVelocity;
    private double lateralVelocity;
    private double approachingVelocity;
    private double distanceToWall;

    ComplexEnemyRobot() {
        super();
    }

    ComplexEnemyRobot(ScannedRobotEvent e, BackAsFrontRobot from) {
        this.update(e, from);
    }

    public void update(ScannedRobotEvent e) throws NotImplementedException {
        throw new NotImplementedException();
    }

    public void update(ScannedRobotEvent e, BackAsFrontRobot robot) {
        super.update(e);
        time = e.getTime();
        AxisRectangle field = robot.getBattleField();

        absBearing = e.getBearingRadians() + Math.toRadians(robot.getHeading());

        x = Physics.getX(robot, absBearing, e.getDistance());
        y = Physics.getY(robot, absBearing, e.getDistance());

        lateralVelocity = Physics.getLateralVelocityFromStationary(absBearing, getVelocity(), getHeading());
        angularVelocity = Physics.getAngularVelocityFromStationary(absBearing, getDistance(), getVelocity(), getHeading());
        approachingVelocity = Physics.getApproachingVelocityFromStationary(absBearing, getVelocity(), getHeading());
        distanceToWall = Math.sqrt(sqr(Math.min(getX(), field.getWidth() - getX())) +
                                    sqr(Math.min(getY(), field.getHeight() - getY())));
    }

    private double sqr(double x) {
        return x*x;
    }

    public long getTime() { return time; }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public double getAngularVelocity() {
        return angularVelocity;
    }

    public double getLateralVelocity() {
        return lateralVelocity;
    }

    public double getApproachingVelocity() {
        return approachingVelocity;
    }

    public MovementPredictor.PredictedPoint getPredictionPoint() {
        return new MovementPredictor.PredictedPoint(getPoint(), getHeading(), getVelocity(), getTime());
    }

    public double getDistanceToWall() {
        return distanceToWall;
    }

    public double getAbsoluteBearing() {
        return absBearing;
    }
}
