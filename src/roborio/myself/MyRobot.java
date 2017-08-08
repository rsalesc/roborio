package roborio.myself;

import roborio.movement.predictor.PredictedPoint;
import roborio.utils.*;
import roborio.utils.geo.AxisRectangle;
import roborio.utils.geo.Point;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class MyRobot {
    private Point position;
    private double heading;
    private double radarHeading;
    private double gunHeading;
    private double velocity;
    private double energy;
    private double gunHeat;
    private double distanceToWall;
    private long time;

    public MyRobot(BackAsFrontRobot robot) {
        this.update(robot);
    }

    public void update(BackAsFrontRobot robot) {
        setPosition(new Point(robot.getX(), robot.getY()));
        setHeading(Math.toRadians(robot.getHeading()));
        setRadarHeading(Math.toRadians(robot.getRadarHeading()));
        setGunHeading(Math.toRadians(robot.getGunHeading()));
        setVelocity(robot.getVelocity());
        setEnergy(robot.getEnergy());
        setGunHeat(robot.getGunHeat());
        setTime(robot.getTime());

        AxisRectangle field = robot.getBattleField();
        distanceToWall = field.distance(getPosition());
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getRadarHeading() {
        return radarHeading;
    }

    public void setRadarHeading(double radarHeading) {
        this.radarHeading = radarHeading;
    }

    public double getGunHeading() {
        return gunHeading;
    }

    public void setGunHeading(double gunHeading) {
        this.gunHeading = gunHeading;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getGunHeat() {
        return gunHeat;
    }

    public void setGunHeat(double gunHeat) {
        this.gunHeat = gunHeat;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Point getPoint() {
        return getPosition();
    }

    public double getLateralVelocity(Point from) {
        double absBearing = Physics.absoluteBearing(from, getPoint());
        return Physics.getLateralVelocityFromStationary(absBearing, getVelocity(), getHeading());
    }

    public double getApproachingVelocity(Point from) {
        double absBearing = Physics.absoluteBearing(from, getPoint());
        return Physics.getApproachingVelocityFromStationary(absBearing, getVelocity(), getHeading());
    }

    public int getDirection(Point from) {
        double lateralVelocity = getLateralVelocity(from);
        return lateralVelocity >= 0 ? 1 : -1;
    }

    public PredictedPoint getPredictionPoint() {
        return new PredictedPoint(getPoint(), getHeading(), getVelocity(), getTime());
    }

    public double getDistanceToWall() {
        return distanceToWall;
    }
}
