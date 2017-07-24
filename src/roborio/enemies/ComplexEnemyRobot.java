package roborio.enemies;

import robocode.Robot;
import robocode.ScannedRobotEvent;
import roborio.movement.predictor.MovementPredictor;
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

    ComplexEnemyRobot() {
        super();
    }

    ComplexEnemyRobot(ScannedRobotEvent e, Robot from) {
        this.update(e, from);
    }

    public void update(ScannedRobotEvent e) throws NotImplementedException {
        throw new NotImplementedException();
    }

    public void update(ScannedRobotEvent e, Robot robot) {
        super.update(e);
        time = e.getTime();

        absBearing = e.getBearingRadians() + Math.toRadians(robot.getHeading());

        x = Physics.getX(robot, absBearing, e.getDistance());
        y = Physics.getY(robot, absBearing, e.getDistance());

        lateralVelocity = Physics.getLateralVelocityFromStationary(absBearing, getVelocity(), getHeading());
        angularVelocity = Physics.getAngularVelocityFromStationary(absBearing, getDistance(), getVelocity(), getHeading());
        approachingVelocity = Physics.getApproachingVelocityFromStationary(absBearing, getVelocity(), getHeading());
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
}
